public class UploadPhotoByHtmlActivity extends HtmlActivity {
  private static final int REQUEST_TAKE_PHOTO = 0x5055;
  private static final int REQUEST_PICK_PHOTO = 0x5555;
  private final int EXTERNAL_PERMISSION_CODE = 0x4351;
  private static final String[] WRITE_EXTERNAL_STORAGE_PERMISSION =
      { Manifest.permission.WRITE_EXTERNAL_STORAGE };
  private ValueCallback<Uri> uploadMessage;
  private ValueCallback<Uri[]> uploadMessageAboveL;
  private PurposeDialog choiceDialog;
  private Uri mTakePhotoUri;
  private boolean isVideo;

  @Override protected void onInit(@Nullable Bundle savedInstanceState) {
    super.onInit(savedInstanceState);
    setDisplayColse(true);
    htmlView.setWebChromeClient(new WebChromeClient() {
      @Override public void onReceivedTitle(WebView view, String title) {
        isVideo = false;
        if (!TextUtils.isEmpty(title)) {
          isVideo = title.contains("FaceID");
          setToolbarTitle(title);
        }
      }

      // 设置网页加载的进度条
      @Override public void onProgressChanged(WebView view, int newProgress) {
        Timber.i("onProgressChanged--" + newProgress);
        //loadingProgressBar.setProgress(newProgress);
        super.onProgressChanged(view, newProgress);
      }

      @Override
      public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
          FileChooserParams fileChooserParams) {
        uploadMessageAboveL = filePathCallback;
        openImageChooserActivity();
        return true;
      }

      // For Android < 3.0
      public void openFileChooser(ValueCallback<Uri> valueCallback) {
        uploadMessage = valueCallback;
        openImageChooserActivity();
      }

      // For Android  >= 3.0
      public void openFileChooser(ValueCallback valueCallback, String acceptType) {
        uploadMessage = valueCallback;
        openImageChooserActivity();
      }

      //For Android  >= 4.1
      public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType,
          String capture) {
        uploadMessage = valueCallback;
        openImageChooserActivity();
      }
    });

    setWebViewClient(new WebViewClient() {
      @Override public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (url.contains("/zxhapp/idCard")) {
          if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            toaster.showText("检测到您的手机系统版本过低，请升级系统后再次尝试");
          }
        }
      }
    });
  }

  private void onChoiceClick(DataDictionaryInfo keyValue) {
    if ("0".equals(keyValue.id)) {
      toImageCapture();
    } else if ("1".equals(keyValue.id)) {
      toImageChoose();
    }
  }

  private void toImageCapture() {
    String[] perms = { Manifest.permission.CAMERA };
    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && EasyPermissions.hasPermissions(this,
        perms)) || PermissionUtils.isCameraPermissionGranted()) {
      if (ExternalStorageUtils.hasExternalStorage()) {
        File photoFile = new File(generatePhotoFilePath(this));
        mTakePhotoUri = null;
        mTakePhotoUri = Uri.fromFile(photoFile);
        try {
          Intent takeIntent = new Intent();
          takeIntent.setAction(
              isVideo ? MediaStore.ACTION_VIDEO_CAPTURE : MediaStore.ACTION_IMAGE_CAPTURE);
          takeIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
          startActivityForResult(takeIntent, REQUEST_TAKE_PHOTO);
        } catch (ActivityNotFoundException e) {
          // ignore
        }
      } else {
        toaster.showText(R.string.sd_card_not_exist);
      }
    } else {
      showPermissionSettingDialog(R.string.rational_camera);
    }
  }

  private void showPermissionSettingDialog(int rationaleResId) {
    new AppSettingsDialog.Builder(this, Phrase.from(this, rationaleResId)
        .put("app_name", getString(R.string.app_name))
        .format()
        .toString()).setTitle(getString(R.string.title_settings_dialog))
        .setPositiveButton(getString(R.string.setting))
        .setNegativeButton(getString(R.string.cancel), null)
        .build()
        .show();
  }

  private String generatePhotoFilePath(Context context) {
    File cacheDir = ExternalStorageUtils.getExternalCacheDir(context, "msxf");
    File tempDir = new File(cacheDir, "photos");
    if (!tempDir.exists()) {
      tempDir.mkdirs();
    }
    return tempDir.getAbsolutePath() + File.separator + System.currentTimeMillis() + (isVideo
        ? ".mp4" : ".jpg");
  }

  private void toImageChoose() {
    if (EasyPermissions.hasPermissions(this, WRITE_EXTERNAL_STORAGE_PERMISSION)) {
      Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
      startActivityForResult(intent, REQUEST_PICK_PHOTO);
    } else {
      String message = Phrase.from(this, R.string.rational_storage)
          .put("app_name", getString(R.string.app_name))
          .format()
          .toString();
      EasyPermissions.requestPermissions(this, message, EXTERNAL_PERMISSION_CODE,
          WRITE_EXTERNAL_STORAGE_PERMISSION);
    }
  }

  private void openImageChooserActivity() {
    if (choiceDialog != null && !choiceDialog.isShowing()) {
      choiceDialog.show();
    } else {
      choiceDialog = new PurposeDialog(this);
      choiceDialog.setTitle("")
          .setNextDesc(getString(R.string.confirm))
          .setCancelViewListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
              cancelUpload();
              choiceDialog.dismiss();
            }
          });
      List<DataDictionaryInfo> choices = new ArrayList<>();
      choices.add(DataDictionaryInfo.builder()
          .setId("0")
          .setDescription(getString(R.string.pick_capture))
          .build());
      choices.add(DataDictionaryInfo.builder()
          .setId("1")
          .setDescription(getString(R.string.pick_choose))
          .build());
      choiceDialog.updateData(choices);
      choiceDialog.setOnItemClickListener(new PurposeAdapter.OnItemClickListener() {
        @Override public void onItemClick(DataDictionaryInfo keyValue) {
          if (keyValue != null) {
            onChoiceClick(keyValue);
            choiceDialog.dismiss();
          }
        }
      });
      choiceDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
        @Override public void onCancel(DialogInterface dialog) {
          cancelUpload();
        }
      });
      choiceDialog.show();
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != RESULT_OK || (null == uploadMessage && null == uploadMessageAboveL)) {
      cancelUpload();
      return;
    }
    Uri result = null;
    if (requestCode == REQUEST_TAKE_PHOTO && mTakePhotoUri != null) {
      result = mTakePhotoUri;
    } else if (requestCode == REQUEST_PICK_PHOTO) {
      result = data.getData();
    }
    if (result != null) {
      onActivityCallBack(result);
    }
  }

  /**
   * 回调到网页
   */
  public void onActivityCallBack(Uri uri) {
    if (uploadMessageAboveL != null) {
      Uri[] uris = new Uri[] { uri };
      uploadMessageAboveL.onReceiveValue(uris);
      uploadMessageAboveL = null;
    } else if (uploadMessage != null) {
      uploadMessage.onReceiveValue(uri);
      uploadMessage = null;
    } else {
      toaster.showText("无法获取数据");
    }
  }

  public void cancelUpload() {
    if (uploadMessageAboveL != null) {
      uploadMessageAboveL.onReceiveValue(null);
    } else if (uploadMessage != null) {
      uploadMessage.onReceiveValue(null);
    }
  }
}
