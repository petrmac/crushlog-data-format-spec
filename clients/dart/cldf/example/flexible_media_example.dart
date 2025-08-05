import 'package:cldf/cldf.dart';

void main() {
  // Example: Converting from CrushLog's single photo/video model
  // to CLDF's flexible media model
  
  // Simulating CrushLog's ClimbMedia
  final hasPhoto = true;
  final photoPath = '/photos/send_photo.jpg';
  final photoAssetId = 'PHAsset123';
  final photoThumbnailPath = '/thumbs/send_photo_thumb.jpg';
  
  final hasVideo = true;
  final videoPath = 'https://example.com/videos/send.mp4';
  final videoAssetId = null; // External URL, no asset ID
  
  // Convert to CLDF flexible media
  final items = <FlexibleMediaItem>[];
  
  if (hasPhoto) {
    items.add(FlexibleMediaItem(
      type: MediaType.photo,
      path: photoPath,
      assetId: photoAssetId,
      thumbnailPath: photoThumbnailPath,
      source: photoAssetId != null ? MediaSource.cloud : MediaSource.local,
      metadata: {
        'width': 3840,
        'height': 2160,
        'size': 2500000,
      },
    ));
  }
  
  if (hasVideo) {
    items.add(FlexibleMediaItem(
      type: MediaType.video,
      path: videoPath,
      source: MediaSource.reference, // External URL
      metadata: {
        'duration': 45.5,
      },
    ));
  }
  
  final media = Media(
    items: items,
    count: items.length,
  );
  
  // Create a climb with the flexible media
  final climb = Climb(
    id: 1,
    date: DateTime.now().toIso8601String().substring(0, 10),
    type: ClimbType.boulder,
    finishType: FinishType.flash,
    routeName: 'Midnight Lightning',
    grades: GradeInfo(
      system: GradeSystem.vScale,
      grade: 'V8',
    ),
    media: media,
    notes: 'Epic send with perfect conditions!',
  );
  
  print('Climb: ${climb.routeName}');
  print('Media items: ${climb.media?.count ?? 0}');
  
  // Serialize to JSON
  final json = climb.toJson();
  print('\nJSON representation:');
  print(json);
  
  // Example of media-only reference (no embedded files)
  final referenceOnlyMedia = Media(
    items: [
      FlexibleMediaItem(
        type: MediaType.photo,
        path: 'DCIM/Camera/IMG_2024.jpg',
        source: MediaSource.reference,
      ),
    ],
    count: 1,
  );
  
  print('\nReference-only media: ${referenceOnlyMedia.toJson()}');
}