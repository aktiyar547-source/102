package com.middleeastcontainer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.middleeastcontainer.ui.activation.ActivationScreen
import com.middleeastcontainer.ui.camera.CameraScreen
import com.middleeastcontainer.ui.camera.CaptureViewModel
import com.middleeastcontainer.ui.delete.DeleteScreen
import com.middleeastcontainer.ui.inventory.InventoryScreen
import com.middleeastcontainer.ui.inventory.SweepScreen
import com.middleeastcontainer.ui.dimension.DimensionScreen
import com.middleeastcontainer.ui.extra.ViewExtraScreen
import com.middleeastcontainer.ui.login.LoginScreen
import com.middleeastcontainer.ui.menu.MenuScreen
import com.middleeastcontainer.ui.ocr.OcrScreen
import com.middleeastcontainer.ui.preview.PreviewScreen
import com.middleeastcontainer.ui.settings.SettingsScreen
import com.middleeastcontainer.ui.singleside.SingleSideScreen
import com.middleeastcontainer.ui.upload.UploadScreen

/** Mirrors the legacy screen flow (see the architecture doc's navigation graph). */
@Composable
fun MecrcNavGraph(navController: NavHostController) {
    // Activation first: without it the app is inert, which is what makes a
    // copied APK useless to whoever has it.
    NavHost(navController = navController, startDestination = Routes.ACTIVATION) {

        composable(Routes.ACTIVATION) {
            ActivationScreen(
                onActivated = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ACTIVATION) { inclusive = true }
                    }
                }
            )
        }


        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { navController.navigate(Routes.MENU) { popUpTo(Routes.LOGIN) { inclusive = true } } }
            )
        }

        composable(Routes.MENU) {
            MenuScreen(
                onNewProject = { navController.navigate(Routes.OCR) },
                onPreview = { navController.navigate(Routes.PREVIEW) },
                onUpload = { navController.navigate(Routes.UPLOAD) },
                onDelete = { navController.navigate(Routes.DELETE) },
                onInventory = { navController.navigate(Routes.INVENTORY) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.OCR) {
            OcrScreen(
                onBack = { navController.popBackStack() },
                // Scans go through the same in-app camera as everything else,
                // so no screen shows the phone's keep/discard prompt.
                onScan = {
                    navController.navigate(Routes.camera("-", CaptureViewModel.OCR))
                },
                onCreated = { container, type ->
                    navController.navigate(Routes.dimension(container, type)) {
                        popUpTo(Routes.OCR) { inclusive = true }
                    }
                },
            )
        }

        composable(
            Routes.DIMENSION,
            arguments = listOf(navArgument("container") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }),
        ) { entry ->
            val container = entry.arguments!!.getString("container")!!
            val type = entry.arguments!!.getString("type")!!
            DimensionScreen(
                container = container, type = type,
                onBackToMenu = { navController.navigate(Routes.MENU) { popUpTo(Routes.MENU) { inclusive = true } } },
                // Straight to the camera — the inspector wants to shoot, not fill a form.
                onOpenSide = { side -> navController.navigate(Routes.camera(container, side)) },
                onEditSide = { side -> navController.navigate(Routes.singleSide(container, type, side)) },
                onAddExtra = { navController.navigate(Routes.camera(container, CaptureViewModel.EXTRA)) },
                onViewExtra = { navController.navigate(Routes.viewExtra(container)) },
            )
        }

        composable(
            Routes.SINGLE_SIDE,
            arguments = listOf(navArgument("container") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType },
                navArgument("side") { type = NavType.StringType }),
        ) { entry ->
            val args = entry.arguments!!
            SingleSideScreen(
                container = args.getString("container")!!,
                type = args.getString("type")!!,
                sideDbName = args.getString("side")!!,
                onDone = { navController.popBackStack() },
            )
        }

        composable(
            Routes.VIEW_EXTRA,
            arguments = listOf(navArgument("container") { type = NavType.StringType }),
        ) { entry ->
            ViewExtraScreen(
                container = entry.arguments!!.getString("container")!!,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            Routes.CAMERA,
            arguments = listOf(
                navArgument("container") { type = NavType.StringType },
                navArgument("target") { type = NavType.StringType },
            ),
        ) {
            CameraScreen(onDone = { navController.popBackStack() })
        }

        composable(Routes.PREVIEW) {
            PreviewScreen(
                onBack = { navController.popBackStack() },
                onOpen = { container, type -> navController.navigate(Routes.dimension(container, type)) },
            )
        }

        composable(Routes.DELETE) { DeleteScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.UPLOAD) { UploadScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.INVENTORY) {
            InventoryScreen(
                onBack = { navController.popBackStack() },
                onOpenSweep = { id -> navController.navigate(Routes.sweep(id)) },
            )
        }

        composable(
            Routes.SWEEP,
            arguments = listOf(navArgument("sweepId") { type = NavType.StringType }),
        ) {
            SweepScreen(onFinished = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}
