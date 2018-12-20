package com.wrbug.developerhelper.shell

import com.jaredrummler.android.shell.CommandResult
import com.wrbug.developerhelper.model.entry.FragmentInfo
import com.wrbug.developerhelper.model.entry.TopActivityInfo
import com.wrbug.developerhelper.util.ShellUtils
import java.util.regex.Pattern


object ShellManager {
    private const val SHELL_TOP_ACTIVITY = "dumpsys activity top"
    private const val SHELL_PROCESS_PID_1 = "ps -ef | grep \"%s\" | grep -v grep | awk '{print \$2}'"
    private const val SHELL_PROCESS_PID_2 = "top -b -n 1 |grep %s |grep -v grep"
    private const val SHELL_PROCESS_PID_3 = "top -n 1 |grep %s |grep -v grep"
    private var SHELL_OPEN_ACCESSiBILITY_SERVICE = arrayOf(
        "settings put secure enabled_accessibility_services com.wrbug.developerhelper/com.wrbug.developerhelper.service.DeveloperHelperAccessibilityService",
        "settings put secure accessibility_enabled 1"
    )

    fun getTopActivity(callback: Callback<TopActivityInfo>) {
        ShellUtils.runWithSu(arrayOf(SHELL_TOP_ACTIVITY), object : ShellUtils.ShellResultCallback() {
            override fun onComplete(result: CommandResult) {
                callback.onSuccess(getTopActivity(result))
            }
        })

    }

    fun getTopActivity(result: CommandResult): TopActivityInfo {
        val stdout = result.getStdout()
        val topActivityInfo = TopActivityInfo()
        val task_s = stdout.split("TASK ")
        for (task_ in task_s) {
            if (task_.contains("mResumed=true")) {
                val regex = "ACTIVITY .* [0-9a-fA-F]+ pid.*"
                val pattern = Pattern.compile(regex)
                val matcher = pattern.matcher(task_)
                if (matcher.find()) {
                    topActivityInfo.activity = matcher.group().split(" ")[1]
                }
                val split =
                    task_.split("\n[ ]{4}[A-Z]".toRegex()).dropLastWhile { it.isEmpty() }
                for (s in split) {
                    if (s.contains("iew Hierarchy")) {
                        for (s1 in s.split("\n".toRegex()).dropLastWhile { it.isEmpty() }) {
                            if (s1.matches(".*\\{.*\\}.*".toRegex())) {
                                val data = s1.substring(s1.indexOf("{") + 1, s1.lastIndexOf("}"))
                                val split1 =
                                    data.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                                if (split1.size != 6 || split1[5].contains("id/").not()) {
                                    continue
                                }
                                topActivityInfo.viewIdHex[split1[5].substring(split1[5].indexOf("id/"))] =
                                        split1[4]
                            }
                        }
                    } else if (s.contains("ocal Activity")) {
                    } else if (s.contains("ctive Fragments")) {
                        val list = ArrayList<FragmentInfo>()
                        val split2 =
                            s.split("#[0-9]+:".toRegex()).dropLastWhile { it.isEmpty() }
                        for (s2 in split2) {
                            val fragmentInfo = FragmentInfo()
                            if (s2.contains("mFragmentId=")) {
                                val name = s2.trim { it <= ' ' }.substring(0, s2.indexOf("{") - 1)
                                list.add(fragmentInfo)
                                fragmentInfo.name = name
                                val split3 =
                                    s2.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                                for (s31 in split3) {
                                    val s3 = s31.replace("\n", "").replace(" ", "")
                                    if (s3.startsWith("mFragmentId=")) {
                                        fragmentInfo.fragmentId = s3.replace("mFragmentId=", "")
                                    } else if (s3.startsWith("mContainerId=")) {
                                        fragmentInfo.containerId = s3.replace("mContainerId=", "")
                                    } else if (s3.startsWith("mTag=")) {
                                        fragmentInfo.tag = s3.replace("mTag=", "")
                                    } else if (s3.startsWith("mState=")) {
                                        fragmentInfo.state = s3.replace("mState=", "").toInt()
                                    } else if (s3.startsWith("mIndex=")) {
                                        fragmentInfo.index = s3.replace("mIndex=", "").toInt()
                                    } else if (s3.startsWith("mWho=")) {
                                        fragmentInfo.who = s3.replace("mWho=", "")
                                    } else if (s3.startsWith("mBackStackNesting=")) {
                                        fragmentInfo.backStackNesting =
                                                s3.replace("mBackStackNesting=", "").toInt()
                                    } else if (s3.startsWith("mAdded=")) {
                                        fragmentInfo.added = s3.replace("mAdded=", "") == "true"
                                    } else if (s3.startsWith("mRemoving=")) {
                                        fragmentInfo.removing = s3.replace("mRemoving=", "") == "true"
                                    } else if (s3.startsWith("mFromLayout=")) {
                                        fragmentInfo.fromLayout = s3.replace("mFromLayout=", "") ==
                                                "true"
                                    } else if (s3.startsWith("mInLayout=")) {
                                        fragmentInfo.inLayout = s3.replace("mInLayout=", "") == "true"
                                    } else if (s3.startsWith("mHidden=")) {
                                        fragmentInfo.hidden = s3.replace("mHidden=", "") == "true"
                                    } else if (s3.startsWith("mDetached=")) {
                                        fragmentInfo.detached = s3.replace("mDetached=", "") == "true"
                                    }
                                }
                            }
                        }
                        topActivityInfo.fragments = list.toTypedArray()
                    }
                }
                break
            }
        }
        return topActivityInfo
    }

    fun getPid(packageName: String): String {
        var result: CommandResult = ShellUtils.runWithSu(String.format(SHELL_PROCESS_PID_1, packageName))
        if (result.isSuccessful) {
            return result.getStdout()
        }
        result = ShellUtils.runWithSu(String.format(SHELL_PROCESS_PID_2, packageName))
        if (result.isSuccessful.not()) {
            result = ShellUtils.runWithSu(String.format(SHELL_PROCESS_PID_3, packageName))
        }
        if (result.isSuccessful.not()) {
            return ""
        }
        return result.getStdout().trim().split(" ")[0]
    }

    fun openAccessibilityService(): Boolean {
        val commandResult = ShellUtils.runWithSu(*SHELL_OPEN_ACCESSiBILITY_SERVICE)
        return commandResult.isSuccessful && commandResult.getStdout().isEmpty()
    }
}