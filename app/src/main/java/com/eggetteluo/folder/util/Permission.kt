package com.eggetteluo.folder.util

import android.Manifest
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX

fun requestStoragePermission(activity: FragmentActivity, onPermissionGranted: () -> Unit) {
    PermissionX.init(activity)
        .permissions(Manifest.permission.MANAGE_EXTERNAL_STORAGE) // 请求所有文件访问权限
        .onExplainRequestReason { scope, deniedList ->
            scope.showRequestReasonDialog(
                deniedList,
                "文件管理器需要访问所有文件以进行管理，请在设置中允许权限",
                "确定", "取消"
            )
        }
        .onForwardToSettings { scope, deniedList ->
            scope.showForwardToSettingsDialog(
                deniedList,
                "您需要去设置界面手动开启文件访问权限",
                "去设置", "取消"
            )
        }
        .request { allGranted, _, _ ->
            if (allGranted) {
                onPermissionGranted() // 权限获取成功，去刷新文件列表
            } else {
                // 处理权限被拒绝的情况，比如弹一个 Toast
            }
        }
}