import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import '../models/post.dart';

class PostGridItem extends StatelessWidget {
  final Post post;
  final VoidCallback onTap;
  final VoidCallback? onLongPress;

  const PostGridItem({
    super.key,
    required this.post,
    required this.onTap,
    this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    final aspectRatio =
        post.height > 0 ? post.width / post.height : 1.0;

    return GestureDetector(
      onTap: onTap,
      onLongPress: onLongPress,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: AspectRatio(
          aspectRatio: aspectRatio.clamp(0.4, 2.5),
          child: CachedNetworkImage(
            imageUrl: post.previewUrl,
            fit: BoxFit.cover,
            placeholder: (context, url) => Container(
              color: Theme.of(context).colorScheme.surfaceContainerHighest,
              child: const Center(
                child: SizedBox(
                  width: 24,
                  height: 24,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
            ),
            errorWidget: (context, url, error) => Container(
              color: Theme.of(context).colorScheme.surfaceContainerHighest,
              child: const Icon(Icons.broken_image_outlined),
            ),
          ),
        ),
      ),
    );
  }
}
