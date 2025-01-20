import 'dart:developer';
import 'dart:io';

import 'package:bubbly/custom_view/common_ui.dart';
import 'package:bubbly/languages/languages_keys.dart';
import 'package:bubbly/modal/user_video/user_video.dart';
import 'package:bubbly/utils/app_res.dart';
import 'package:bubbly/utils/assert_image.dart';
import 'package:bubbly/utils/colors.dart';
import 'package:bubbly/utils/const_res.dart';
import 'package:bubbly/utils/font_res.dart';
import 'package:bubbly/utils/my_loading/my_loading.dart';
import 'package:bubbly/utils/url_res.dart';
import 'package:bubbly_camera/bubbly_camera.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:ffmpeg_kit_flutter_full/ffmpeg_kit.dart';
import 'package:ffmpeg_kit_flutter_full/return_code.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_branch_sdk/flutter_branch_sdk.dart';
import 'package:get/get.dart';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:provider/provider.dart';
import 'package:saver_gallery/saver_gallery.dart';
import 'package:screenshot/screenshot.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

class SocialLinkShareSheet extends StatefulWidget {
  final Data videoData;

  const SocialLinkShareSheet({Key? key, required this.videoData});

  @override
  State<SocialLinkShareSheet> createState() => _SocialLinkShareSheetState();
}

class _SocialLinkShareSheetState extends State<SocialLinkShareSheet> {
  List<String> shareIconList = [icDownloads, icWhatsapp, icInstagram, icCopy, icMore];
  bool androidExistNotSave = false;
  ScreenshotController screenshotController = ScreenshotController();

  @override
  Widget build(BuildContext context) {
    return Consumer(
      builder: (context, MyLoading myLoading, child) {
        return Wrap(
          children: [
            Stack(
              alignment: Alignment.bottomCenter,
              children: [
                Screenshot(
                  controller: screenshotController,
                  child: Column(
                    children: [
                      Image.asset(
                        myLoading.isDark ? icLogo : icLogoLight,
                        width: 30,
                        fit: BoxFit.fitHeight,
                      ),
                      Text(
                        '@${widget.videoData.userName ?? appName}',
                        style: TextStyle(
                          fontSize: 10,
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  width: double.infinity,
                  margin: EdgeInsets.only(top: AppBar().preferredSize.height),
                  padding: EdgeInsets.symmetric(horizontal: 15, vertical: 10),
                  decoration: BoxDecoration(
                      color: myLoading.isDark ? ColorRes.colorPrimary : ColorRes.white,
                      borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
                  child: Column(
                    children: [
                      Stack(
                        alignment: Alignment.center,
                        children: [
                          Text(
                            LKey.shareThisVideo.tr,
                            style: TextStyle(fontFamily: FontRes.fNSfUiMedium, fontSize: 16),
                          ),
                          Align(
                            alignment: Alignment.centerRight,
                            child: IconButton(
                                onPressed: () {
                                  Navigator.pop(context);
                                },
                                icon: Icon(Icons.close)),
                          )
                        ],
                      ),
                      Divider(color: ColorRes.colorTextLight),
                      Wrap(
                        children: List.generate(shareIconList.length, (index) {
                          return InkWell(
                            onTap: () => _onTap(index),
                            child: Container(
                              height: 40,
                              width: 40,
                              padding: EdgeInsets.all(10),
                              margin: EdgeInsets.all(8),
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                gradient: LinearGradient(
                                  begin: Alignment.topCenter,
                                  end: Alignment.bottomCenter,
                                  colors: [ColorRes.colorTheme, ColorRes.colorIcon],
                                ),
                              ),
                              child: Image.asset(
                                shareIconList[index],
                                color: ColorRes.white,
                              ),
                            ),
                          );
                        }),
                      ),
                      SizedBox(height: AppBar().preferredSize.height)
                    ],
                  ),
                ),
              ],
            )
          ],
        );
      },
    );
  }

  Future<void> _onTap(int index) async {
    HapticFeedback.mediumImpact();
    Navigator.pop(context);

    Uint8List? imageFile = await screenshotController.capture();
    if (imageFile != null) {
      print('Screenshot captured: $imageFile');
    }

    final sharedLink = await _shareBranchLink();
    Get.dialog(LoaderDialog());

    switch (index) {
      case 0:
        addWatermarkToVideo(imageFile);
        break;
      case 1:
        _share('whatsapp', sharedLink);
        break;
      case 2:
        if (Platform.isIOS) {
          _share('instagram', sharedLink);
        } else {
          BubblyCamera.shareToInstagram(sharedLink);
        }
        break;
      case 3:
        Clipboard.setData(ClipboardData(text: sharedLink));
        break;
      case 4:
        Share.share(
          AppRes.checkOutThisAmazingProfile(sharedLink),
          subject: '${AppRes.look} ${widget.videoData.userName}',
        );
        break;
    }

    Get.back();
  }

  void _share(String platform, String sharedLink) async {
    final uri = Uri.parse(
        platform == 'whatsapp' ? 'whatsapp://send?text=$sharedLink' : 'instagram://sharesheet?text=$sharedLink');

    if (!await launchUrl(uri)) {
      // Handle failure to launch URL
    }
  }

  Future<String> _shareBranchLink() async {
    // Creating BranchUniversalObject
    BranchUniversalObject buo = BranchUniversalObject(
      canonicalIdentifier: 'flutter/branch',
      title: widget.videoData.userName ?? '',
      imageUrl: ConstRes.itemBaseUrl + widget.videoData.postImage!,
      contentDescription: '',
      publiclyIndex: true,
      locallyIndex: true,
      contentMetadata: BranchContentMetaData()..addCustomMetadata(UrlRes.postId, widget.videoData.postId),
    );

    // Creating BranchLinkProperties
    BranchLinkProperties lp = BranchLinkProperties(
      channel: 'facebook',
      feature: 'sharing',
      stage: 'new share',
      tags: ['one', 'two', 'three'],
    )
      ..addControlParam('url', 'http://www.google.com')
      ..addControlParam('url2', 'http://flutter.dev');

    // Getting Short URL
    BranchResponse response = await FlutterBranchSdk.getShortUrl(buo: buo, linkProperties: lp);
    return response.success ? response.result : '';
  }

  Future<void> addWatermarkToVideo(Uint8List? pngBytes) async {
    CommonUI.showToast(msg: '${LKey.videoDownloadingStarted.tr}');

    // Capture the watermark PNG
    File? waterMarkPath = await _capturePng(pngBytes);

    if (waterMarkPath == null) {
      log('Watermark not generated');
      return;
    }

    // Get the video file path
    File videoUrlPath = await _getFilePathFromUrl(url: '${ConstRes.itemBaseUrl}${widget.videoData.postVideo}');

    // Prepare output file path
    final directory = await getApplicationDocumentsDirectory();
    final outputPath = '${directory.path}/${DateTime.now().millisecondsSinceEpoch}.mp4';

    // Execute FFmpeg command to overlay watermark on the video
    final session = await FFmpegKit.execute(
        '-i ${videoUrlPath.path} -i ${waterMarkPath.path} -filter_complex "[1][0]scale2ref'
        '=w=\'iw*15/100\':h=\'ow/mdar\'[wm][vid];[vid][wm]overlay=x=(main_w-overlay_w-10):y=(main_h-overlay_h-10)" -qscale 0 -y $outputPath');

    // Delete videoUrlPath from Storage
    File(videoUrlPath.path).delete().then((value) => print('Delete videoUrlPath : $videoUrlPath'));

    // Check the return code of the FFmpeg session
    final returnCode = await session.getReturnCode();
    if (ReturnCode.isSuccess(returnCode)) {
      await saveToGallery(outputPath);
    } else {
      log('Video not saved in gallery');
    }
  }

  Future<File?> _capturePng(Uint8List? pngBytes) async {
    if (pngBytes == null) {
      print('Screenshot not captured');
      return null;
    }

    final directory = (await getApplicationDocumentsDirectory()).path;
    final filePath = '$directory/${DateTime.now().millisecondsSinceEpoch}.png';
    final imgFile = File(filePath);
    return await imgFile.writeAsBytes(pngBytes);
  }

  Future<File> _getFilePathFromUrl({required String url}) async {
    try {
      final response = await http.get(Uri.parse(url));
      if (response.statusCode != 200) {
        throw Exception("Failed to fetch URL: ${response.statusCode}");
      }

      final bytes = response.bodyBytes;
      final dir = await getApplicationDocumentsDirectory();
      final filePath = "${dir.path}/${DateTime.now().millisecondsSinceEpoch}.mp4";

      final file = File(filePath);
      await file.writeAsBytes(bytes);
      return file;
    } catch (e) {
      throw Exception("Error downloading and saving file: $e");
    }
  }

  Future<void> saveToGallery(String outputPath) async {
    bool isGranted;

    if (Platform.isAndroid) {
      final deviceInfoPlugin = DeviceInfoPlugin();
      final deviceInfo = await deviceInfoPlugin.androidInfo;
      final sdkInt = deviceInfo.version.sdkInt;

      isGranted = (androidExistNotSave)
          ? await (sdkInt > 33 ? Permission.photos : Permission.storage).request().isGranted
          : sdkInt < 29
              ? await Permission.storage.request().isGranted
              : true;
    } else {
      isGranted = await Permission.photosAddOnly.request().isGranted;
    }

    print('Permission Is: $isGranted');

    if (isGranted) {
      try {
        final result = await SaverGallery.saveFile(
          filePath: outputPath,
          fileName: '${DateTime.now().millisecondsSinceEpoch}.mp4',
          androidRelativePath: "Movies",
          skipIfExists: false,
        );
        print('🎞️ Save result: $result');
        CommonUI.showToast(msg: 'Video successfully saved to the gallery. ');
      } catch (e) {
        print('Error while saving file: $e');
      } finally {
        // Optionally delete the output file after saving
        await File(outputPath).delete().then(
          (value) {
            print('Deleted: $outputPath');
          },
        );
      }
    } else {
      print('Permission denied. Cannot save the file.');
    }
  }
}
