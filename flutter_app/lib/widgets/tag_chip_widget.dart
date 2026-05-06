import 'package:flutter/material.dart';

/// Tag type constants matching yande.re API values.
class TagType {
  static const int general = 0;
  static const int artist = 1;
  static const int copyright = 3;
  static const int character = 4;
}

class TagChipWidget extends StatelessWidget {
  final String tag;
  final int tagType;
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;

  const TagChipWidget({
    super.key,
    required this.tag,
    this.tagType = TagType.general,
    this.onTap,
    this.onLongPress,
  });

  Color _backgroundColor(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    switch (tagType) {
      case TagType.artist:
        return Colors.red.shade800.withOpacity(0.15);
      case TagType.copyright:
        return Colors.purple.shade700.withOpacity(0.15);
      case TagType.character:
        return Colors.green.shade700.withOpacity(0.15);
      default:
        return cs.surfaceContainerHighest;
    }
  }

  Color _labelColor(BuildContext context) {
    switch (tagType) {
      case TagType.artist:
        return Colors.red.shade300;
      case TagType.copyright:
        return Colors.purple.shade300;
      case TagType.character:
        return Colors.green.shade300;
      default:
        return Theme.of(context).colorScheme.onSurface;
    }
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      onLongPress: onLongPress,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        decoration: BoxDecoration(
          color: _backgroundColor(context),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: _labelColor(context).withOpacity(0.4),
            width: 1,
          ),
        ),
        child: Text(
          tag.replaceAll('_', ' '),
          style: TextStyle(
            color: _labelColor(context),
            fontSize: 12,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }
}
