package com.samourai.wallet.fragments;

import static com.samourai.wallet.permissions.PermissionsUtil.CAMERA_PERMISSION_CODE;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.transition.MaterialSharedAxis;
import com.invertedx.hummingbird.QRScanner;
import com.samourai.wallet.R;
import com.sparrowwallet.hummingbird.registry.RegistryType;

import org.spongycastle.util.encoders.Hex;

import kotlin.Unit;


public class CameraFragmentBottomSheet extends BottomSheetDialogFragment {

    private ListenQRScan listenQRScan;
    private MaterialCardView permissionView;
    private QRScanner scannerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_camera, null);
        permissionView = view.findViewById(R.id.permissionCameraDialog);
        scannerView = view.findViewById(R.id.scanner_view);
        permissionView.findViewById(R.id.permissionCameraDialogGrantBtn).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scannerView.setQRDecodeListener(resultString -> {
            getActivity().runOnUiThread(() -> {
                if (this.listenQRScan != null) {
                    this.listenQRScan.onDetect(resultString);
                }
            });
            return Unit.INSTANCE;
        });
        scannerView.setURDecodeListener((bytes, type) -> {
            if (type == RegistryType.CRYPTO_PSBT) {
                getActivity().runOnUiThread(() -> {
                    if (this.listenQRScan != null) {
                        this.listenQRScan.onDetect(Hex.toHexString(bytes));
                        scannerView.stopScanner();
                    }
                });
            } else {
                getActivity().runOnUiThread(() -> {
                    if (this.listenQRScan != null) {
                        this.listenQRScan.onDetect("");
                        scannerView.stopScanner();
                    }
                });
            }

            return Unit.INSTANCE;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        startCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        startCamera();
    }

    void startCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            scannerView.startScanner();
            showPermissionView(false);
        } else {
            showPermissionView(true);
        }
    }

    public void showPermissionView(Boolean show) {
        MaterialSharedAxis sharedAxis = new MaterialSharedAxis(MaterialSharedAxis.Y, show);

        TransitionManager.beginDelayedTransition((ViewGroup) permissionView.getRootView(), sharedAxis);
        if (show) {
            permissionView.setVisibility(View.VISIBLE);
            scannerView.setVisibility(View.INVISIBLE);
        } else {
            permissionView.setVisibility(View.INVISIBLE);
            scannerView.setVisibility(View.VISIBLE);
        }

    }

    public void setQrCodeScanListener(ListenQRScan listenQRScan) {
        this.listenQRScan = listenQRScan;
    }

    @Override
    public void onPause() {
        scannerView.stopScanner();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        scannerView.stopScanner();
        super.onDestroy();
    }

    public interface ListenQRScan {
        void onDetect(String code);
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null) {
            view.post(() -> {
                View parent = (View) view.getParent();
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) (parent).getLayoutParams();
                CoordinatorLayout.Behavior behavior = params.getBehavior();
                BottomSheetBehavior bottomSheetBehavior = (BottomSheetBehavior) behavior;
                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.setPeekHeight(view.getMeasuredHeight());
                }

            });
        }
    }
}