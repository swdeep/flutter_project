# Shortzz

# Date: 23 October 2024

## Summary

- Reel download issue fixed
- Video preview orientation corrected[README.md](..%2Fbubbly%202%2FREADME.md)
- Bug fixes and performance improvements

#### Updated Files
[README.md](..%2Fbubbly%202%2FREADME.md)
- [settings.gradle](android/settings.gradle)
- [pubspec.yaml](pubspec.yaml)
- Podfile
- api_service.dart
- camera_screen.dart
- const_res.dart
- english_en.dart
- item_video.dart
- login_sheet.dart
- preview_screen.dart
- share_sheet.dart
- wallet_screen.dart
- for_u_screen.dart
- item_following.dart
- main.dart
- preview_screen.dart
- upload_screen.dart
- video_list_screen.dart
- video_view.dart
- videos_by_hashtag.dart

#### Added Files

- None

#### Deleted Files

- None

----------------------------------------------------------------------------------------------------

# Date: 19 September 2024

## Summary

- replace library `mobile_scanner: ^5.2.3`  to `qr_code_scanner_plus: ^2.0.6`
- comment bug fixed
- upload sheet add `DetectableTextEditingController`
- Update pubspec.yaml file some library

#### Updated Files
- comment_screen.dart
- pubspec.yaml
- scan_qr_code_screen.dart
- upload_screen.dart

#### Added Files
None

#### Deleted Files
None

----------------------------------------------------------------------------------------------------

# Date: 1 August 2024

## Summary

- Remove Library - `carousel_slider`
- Change Library -
-
Remove `image_gallery_saver` Add `saver_gallery`   
  Remove `qr_code_scanner` Add `mobile_scanner`

#### Updated Files

- [AndroidManifest.xml](android/app/src/main/AndroidManifest.xml)
- [Info.plist](ios/Runner/Info.plist)
- [pubspec.yaml](pubspec.yaml)
- [settings.gradle](android/settings.gradle)
- main.dart
- app_res.dart
- scan_qr_code_screen.dart
- camera_screen.dart
- following_screen.dart
- gift_sheet.dart
- languages_keys.dart
- share_sheet.dart
- arabic_ar.dart
- chinese_zh.dart
- danish_da.dart
- dutch_nl.dart
- english_en.dart
- france_fr.dart
- german_de.dart
- greek_el.dart
- hindi_hi.dart
- indonesian_id.dart
- japanese_ja.dart
- korean_ko.dart
- norwegian_bokmal_nb.dart
- polish_pl.dart
- portuguese_pt.dart
- russian_ru.dart
- spanish_es.dart
- thai_th.dart
- turkish_tr.dart
- vietnamese_vi.dart

#### Added Files

None

#### Deleted Files

- messages_all.dart
- messages_en.dart
- intl_en.arb
- l10n.dart

----------------------------------------------------------------------------------------------------

# Date: 19 June 2024

## Summary
- Apple Sign in
- Comment sheet loader
- `const_res.dart` file add video limit maxUploadMB and maxUploadSecond

#### Updated Files
- [pubspec.yaml](pubspec.yaml)
- [build.gradle](android/app/build.gradle)
- api_service.dart
- camera_screen.dart
- comment_screen.dart
- common_ui.dart
- const_res.dart
- login_sheet.dart

#### Added Files

None

#### Deleted Files

None

----------------------------------------------------------------------------------------------------

# Date: 12 June 2024

## Summary
- User Camera Don't Allow Permission Screen

#### Updated Files
- [AppDelegate.swift](ios/Runner/AppDelegate.swift)
- [Info.plist](ios/Runner/Info.plist)
- [Podfile](ios/Podfile)
- [pubspec.yaml](pubspec.yaml)
- [SwiftFlutterDailogPlugin.swift](CameraPlugin/camera/ios/Classes/SwiftFlutterDailogPlugin.swift)
- assert_image.dart
- camera_screen.dart
- end_user_license_agreement.dart
- item_video.dart
- live_stream_view_model.dart
- main.dart
- main_screen.dart
- settings.gradle
- share_sheet.dart
- videos_by_sound.dart

#### Added Files

- ic_camera_permission.png
- microphone.png
- no-video.png

#### Deleted Files

- bubble_corner.png
- bubble_single.png
- bubble_single_small.png
- bubbles.png
- bubbles_small.png
- camera.jpg
- idol.jpg
- malaika.jpg

----------------------------------------------------------------------------------------------------

# Date: 10 May 2024

## Summary
- Add In App Purchase
- Migrate Gradle Files
- Library update
- Remove app_tracking_transparency Library and Add Consent Form for Ad Mob

#### Updated Files
- api_service.dart
- common_ui.dart
- dialog_coins_plan.dart
- wallet_screen.dart
- upload_screen.dart
- webview_screen.dart
- item_video.dart
- login_sheet.dart
- main.dart
- main_screen.dart
- broad_cast_screen_view_model.dart
- end_user_license_agreement.dart
- camera_screen.dart
- [AndroidManifest.xml](/android/app/src/debug/AndroidManifest.xml)
- [AndroidManifest.xml](/android/app/src/main/AndroidManifest.xml)
- [AndroidManifest.xml](/android/app/src/profile/AndroidManifest.xml)
- [BubblyCameraPlugin.kt](/CameraPlugin/camera/android/src/main/kotlin/com/retrytech/bubbly_camera)
- [build.gradle](/android/app/build.gradle)
- [build.gradle](/android)
- [build.gradle](/CameraPlugin/camera/android/build.gradle)
- [CameraXView.kt](/CameraPlugin/camera/android/src/main/kotlin/com/retrytech/bubbly_camera/CameraXView.kt)
- [gradle.properties](/android/gradle.properties)
- [gradle-wrapper.properties](/android/gradle/wrapper/gradle-wrapper.properties)
- [MainActivity.kt](/android/app/src/main/kotlin/com/retrytech/bubbly/MainActivity.kt)
- [settings.gradle](/android/settings.gradle)
- pubspec.yaml

#### Added Files
- proguard-rules.pro
- [ads_service.dart](/lib/service/ads_service.dart)

#### Deleted Files
- MyPlayStoreBilling.java