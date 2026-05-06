import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:photo_view/photo_view.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../models/post.dart';
import '../providers/blacklist_provider.dart';
import '../providers/favorite_tags_provider.dart';
import '../providers/favorites_provider.dart';
import '../widgets/tag_chip_widget.dart';

class DetailScreen extends StatefulWidget {
  final List<Post> posts;
  final int initialIndex;

  const DetailScreen({
    super.key,
    required this.posts,
    required this.initialIndex,
  });

  @override
  State<DetailScreen> createState() => _DetailScreenState();
}

class _DetailScreenState extends State<DetailScreen> {
  late final PageController _pageController;
  late int _currentIndex;
  bool _immersive = false;

  @override
  void initState() {
    super.initState();
    _currentIndex = widget.initialIndex;
    _pageController = PageController(initialPage: widget.initialIndex);
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  Post get _currentPost => widget.posts[_currentIndex];

  void _toggleImmersive() {
    setState(() => _immersive = !_immersive);
  }

  Future<void> _openSource(String? source) async {
    if (source == null || source.isEmpty) return;
    String effectiveUrl = source;
    if (!source.startsWith('http')) {
      effectiveUrl = 'https://$source';
    }
    final uri = Uri.tryParse(effectiveUrl);
    if (uri != null && await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } else {
      if (mounted) {
        showDialog(
          context: context,
          builder: (_) => AlertDialog(
            title: const Text('来源'),
            content: SelectableText(source),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('关闭'),
              ),
            ],
          ),
        );
      }
    }
  }

  void _showTagOptions(BuildContext context, String tag) {
    showModalBottomSheet(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.search),
              title: const Text('搜索该标签'),
              onTap: () {
                Navigator.pop(ctx);
                Navigator.popUntil(context, (r) => r.isFirst);
                // Navigate back to home and trigger search
              },
            ),
            ListTile(
              leading: const Icon(Icons.copy),
              title: const Text('复制标签'),
              onTap: () {
                Navigator.pop(ctx);
                Clipboard.setData(ClipboardData(text: tag));
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('已复制到剪贴板')),
                );
              },
            ),
            ListTile(
              leading: const Icon(Icons.block),
              title: const Text('加入黑名单'),
              onTap: () {
                Navigator.pop(ctx);
                context.read<BlacklistProvider>().add(tag);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('已将 $tag 加入黑名单')),
                );
              },
            ),
            ListTile(
              leading: const Icon(Icons.label),
              title: const Text('收藏标签'),
              onTap: () {
                Navigator.pop(ctx);
                context.read<FavoriteTagsProvider>().add(tag);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('已收藏标签 $tag')),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTagSection(BuildContext context, Post post) {
    final tagList = post.tagList;
    if (tagList.isEmpty) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.all(12),
      child: Wrap(
        spacing: 6,
        runSpacing: 6,
        children: tagList.map((tag) {
          return TagChipWidget(
            tag: tag,
            tagType: TagType.general,
            onTap: () {
              // Search tag: go back to home with query
              Navigator.popUntil(context, (r) => r.isFirst);
            },
            onLongPress: () => _showTagOptions(context, tag),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildInfoRow(BuildContext context, Post post) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      child: Row(
        children: [
          Text(
            'ID: ${post.id}',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(width: 12),
          Text(
            '${post.width}×${post.height}',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(width: 12),
          _ratingBadge(post.rating),
          const Spacer(),
          if (post.source?.isNotEmpty == true)
            IconButton(
              icon: const Icon(Icons.link, size: 20),
              tooltip: '来源',
              onPressed: () => _openSource(post.source),
            ),
        ],
      ),
    );
  }

  Widget _ratingBadge(String rating) {
    final (label, color) = switch (rating) {
      's' => ('Safe', Colors.green),
      'q' => ('Q', Colors.orange),
      'e' => ('Explicit', Colors.red),
      _ => (rating.toUpperCase(), Colors.grey),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withOpacity(0.2),
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: color.withOpacity(0.5)),
      ),
      child: Text(
        label,
        style: TextStyle(fontSize: 11, color: color, fontWeight: FontWeight.w600),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          // Main pager
          PageView.builder(
            controller: _pageController,
            itemCount: widget.posts.length,
            onPageChanged: (i) => setState(() => _currentIndex = i),
            itemBuilder: (context, index) {
              final post = widget.posts[index];
              return _PostPage(
                post: post,
                onTap: _toggleImmersive,
              );
            },
          ),

          // Top bar (hidden in immersive mode)
          if (!_immersive)
            Positioned(
              top: 0,
              left: 0,
              right: 0,
              child: AppBar(
                backgroundColor: Colors.black54,
                foregroundColor: Colors.white,
                title: Text(
                  '${_currentIndex + 1} / ${widget.posts.length}',
                  style: const TextStyle(fontSize: 14),
                ),
              ),
            ),

          // Bottom info panel (hidden in immersive mode)
          if (!_immersive)
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Container(
                constraints: const BoxConstraints(maxHeight: 300),
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.bottomCenter,
                    end: Alignment.topCenter,
                    colors: [Colors.black87, Colors.transparent],
                  ),
                ),
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _buildInfoRow(context, _currentPost),
                      _buildTagSection(context, _currentPost),
                      const SizedBox(height: 8),
                    ],
                  ),
                ),
              ),
            ),

          // FAB — favorite button
          if (!_immersive)
            Positioned(
              bottom: 16,
              right: 16,
              child: _FavoriteFab(post: _currentPost),
            ),
        ],
      ),
    );
  }
}

// ── Individual page with PhotoView ──────────────────────────────

class _PostPage extends StatelessWidget {
  final Post post;
  final VoidCallback onTap;

  const _PostPage({required this.post, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: PhotoView(
        imageProvider: CachedNetworkImageProvider(post.fileUrl),
        loadingBuilder: (context, event) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              CachedNetworkImage(
                imageUrl: post.previewUrl,
                fit: BoxFit.contain,
                width: double.infinity,
              ),
              const SizedBox(height: 8),
              if (event != null &&
                  event.expectedTotalBytes != null &&
                  event.expectedTotalBytes! > 0)
                LinearProgressIndicator(
                  value: event.cumulativeBytesLoaded /
                      event.expectedTotalBytes!,
                  minHeight: 2,
                )
              else
                const LinearProgressIndicator(minHeight: 2),
            ],
          ),
        ),
        errorBuilder: (context, error, stack) => const Center(
          child: Icon(Icons.broken_image_outlined,
              color: Colors.white54, size: 64),
        ),
        minScale: PhotoViewComputedScale.contained,
        maxScale: PhotoViewComputedScale.covered * 4,
        backgroundDecoration: const BoxDecoration(color: Colors.black),
        heroAttributes: PhotoViewHeroAttributes(tag: 'post_${post.id}'),
      ),
    );
  }
}

// ── Favorite FAB ─────────────────────────────────────────────────

class _FavoriteFab extends StatelessWidget {
  final Post post;

  const _FavoriteFab({required this.post});

  @override
  Widget build(BuildContext context) {
    final favProvider = context.watch<FavoritesProvider>();
    final isFav = favProvider.isFavorite(post.id);

    return FloatingActionButton(
      heroTag: 'favorite_fab',
      onPressed: () {
        if (isFav) {
          favProvider.remove(post.id);
          ScaffoldMessenger.of(context)
              .showSnackBar(const SnackBar(content: Text('已取消收藏')));
        } else {
          favProvider.add(post);
          ScaffoldMessenger.of(context)
              .showSnackBar(const SnackBar(content: Text('已添加到收藏')));
        }
      },
      backgroundColor: isFav ? Colors.pink.shade400 : null,
      child: Icon(
        isFav ? Icons.favorite : Icons.favorite_border,
        color: isFav ? Colors.white : null,
      ),
    );
  }
}
