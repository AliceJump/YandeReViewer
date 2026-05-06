import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';
import 'package:provider/provider.dart';

import '../api/yande_api.dart';
import '../models/tag_info.dart';
import '../providers/favorite_tags_provider.dart';
import '../providers/post_provider.dart';
import '../widgets/post_grid_item.dart';
import '../widgets/tag_chip_widget.dart';
import 'blacklist_screen.dart';
import 'detail_screen.dart';
import 'favorite_tags_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final ScrollController _scrollController = ScrollController();
  final TextEditingController _searchController = TextEditingController();

  List<TagInfo> _suggestions = [];
  bool _showSuggestions = false;
  bool _isLoadingSuggestions = false;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    // Initial load
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<PostProvider>().refresh();
    });
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  void _onScroll() {
    final provider = context.read<PostProvider>();
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 400) {
      provider.loadMore();
    }
  }

  Future<void> _fetchSuggestions(String text) async {
    final parts = text.split(' ');
    final last = parts.last;
    if (last.isEmpty) {
      setState(() {
        _suggestions = [];
        _showSuggestions = false;
      });
      return;
    }
    setState(() => _isLoadingSuggestions = true);
    try {
      final results = await YandeApi.instance.searchTags(last);
      if (mounted) {
        setState(() {
          _suggestions = results;
          _showSuggestions = results.isNotEmpty;
          _isLoadingSuggestions = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoadingSuggestions = false);
    }
  }

  void _selectSuggestion(String suggestion) {
    final parts = _searchController.text.split(' ');
    parts[parts.length - 1] = suggestion;
    _searchController.text = '${parts.join(' ')} ';
    _searchController.selection = TextSelection.fromPosition(
      TextPosition(offset: _searchController.text.length),
    );
    setState(() {
      _suggestions = [];
      _showSuggestions = false;
    });
  }

  void _submitSearch() {
    final query = _searchController.text.trim();
    setState(() {
      _showSuggestions = false;
      _suggestions = [];
    });
    context.read<PostProvider>().search(query);
  }

  void _scrollToTop() {
    _scrollController.animateTo(
      0,
      duration: const Duration(milliseconds: 500),
      curve: Curves.easeInOut,
    );
  }

  Widget _buildDrawer(BuildContext context) {
    final favTags = context.watch<FavoriteTagsProvider>().tags;
    final provider = context.watch<PostProvider>();

    return Drawer(
      child: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DrawerHeader(
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primaryContainer,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'YandeReViewer',
                    style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Flutter Edition',
                    style: TextStyle(
                      color: Theme.of(context)
                          .colorScheme
                          .onPrimaryContainer
                          .withOpacity(0.7),
                    ),
                  ),
                ],
              ),
            ),

            // Mode switcher
            ListTile(
              leading: Icon(
                provider.mode == BrowseMode.normal
                    ? Icons.public
                    : Icons.favorite,
              ),
              title: Text(
                provider.mode == BrowseMode.normal ? '浏览模式' : '收藏模式',
              ),
              trailing: Switch(
                value: provider.mode == BrowseMode.favorites,
                onChanged: (val) {
                  provider.setMode(
                    val ? BrowseMode.favorites : BrowseMode.normal,
                  );
                  Navigator.pop(context);
                },
              ),
            ),

            const Divider(),

            // Navigation items
            ListTile(
              leading: const Icon(Icons.label_outline),
              title: const Text('收藏标签'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) => const FavoriteTagsScreen(),
                  ),
                );
              },
            ),
            ListTile(
              leading: const Icon(Icons.block),
              title: const Text('黑名单'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) => const BlacklistScreen(),
                  ),
                );
              },
            ),

            if (favTags.isNotEmpty) ...[
              const Divider(),
              Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 4,
                ),
                child: Text(
                  '收藏标签',
                  style: Theme.of(context).textTheme.labelMedium,
                ),
              ),
              Expanded(
                child: ListView.builder(
                  padding: EdgeInsets.zero,
                  itemCount: favTags.length,
                  itemBuilder: (context, i) {
                    final tag = favTags[i];
                    return ListTile(
                      dense: true,
                      leading: const Icon(Icons.label, size: 18),
                      title: Text(
                        tag.replaceAll('_', ' '),
                        style: const TextStyle(fontSize: 13),
                      ),
                      onTap: () {
                        _searchController.text = tag;
                        context.read<PostProvider>().search(tag);
                        Navigator.pop(context);
                      },
                      onLongPress: () => _showTagContextMenu(context, tag),
                    );
                  },
                ),
              ),
            ] else
              const Expanded(child: SizedBox()),
          ],
        ),
      ),
    );
  }

  void _showTagContextMenu(BuildContext context, String tag) {
    showModalBottomSheet(
      context: context,
      builder: (_) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.copy),
              title: const Text('复制标签'),
              onTap: () {
                Navigator.pop(context);
                Clipboard.setData(ClipboardData(text: tag));
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('已复制到剪贴板')),
                );
              },
            ),
            ListTile(
              leading: const Icon(Icons.label_off_outlined),
              title: const Text('取消收藏'),
              onTap: () {
                Navigator.pop(context);
                context.read<FavoriteTagsProvider>().remove(tag);
              },
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<PostProvider>();

    return Scaffold(
      drawer: _buildDrawer(context),
      appBar: AppBar(
        title: _buildSearchBar(context),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: '刷新',
            onPressed: () => context.read<PostProvider>().refresh(),
          ),
        ],
      ),
      body: Column(
        children: [
          _buildRatingFilter(context, provider),
          Expanded(
            child: Stack(
              children: [
                _buildGrid(context, provider),
                if (_showSuggestions)
                  _buildSuggestionOverlay(context),
              ],
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.small(
        onPressed: _scrollToTop,
        tooltip: '回到顶部',
        child: const Icon(Icons.arrow_upward),
      ),
    );
  }

  Widget _buildSearchBar(BuildContext context) {
    return TextField(
      controller: _searchController,
      decoration: const InputDecoration(
        hintText: '搜索标签...',
        border: InputBorder.none,
        contentPadding: EdgeInsets.zero,
      ),
      textInputAction: TextInputAction.search,
      onChanged: _fetchSuggestions,
      onSubmitted: (_) => _submitSearch(),
    );
  }

  Widget _buildRatingFilter(BuildContext context, PostProvider provider) {
    const ratings = [
      ('s', 'Safe'),
      ('q', 'Questionable'),
      ('e', 'Explicit'),
    ];
    return Container(
      height: 44,
      padding: const EdgeInsets.symmetric(horizontal: 12),
      child: Row(
        children: ratings.map((r) {
          final active = provider.ratings.contains(r.$1);
          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: FilterChip(
              label: Text(r.$2, style: const TextStyle(fontSize: 12)),
              selected: active,
              onSelected: (_) => provider.toggleRating(r.$1),
              visualDensity: VisualDensity.compact,
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildGrid(BuildContext context, PostProvider provider) {
    if (provider.posts.isEmpty && provider.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (provider.posts.isEmpty && provider.error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 48),
            const SizedBox(height: 8),
            Text(provider.error!),
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: () => provider.refresh(),
              icon: const Icon(Icons.refresh),
              label: const Text('重试'),
            ),
          ],
        ),
      );
    }
    if (provider.posts.isEmpty) {
      return const Center(child: Text('暂无内容'));
    }

    final posts = provider.posts;

    return RefreshIndicator(
      onRefresh: () async => provider.refresh(),
      child: MasonryGridView.count(
        controller: _scrollController,
        crossAxisCount: 2,
        mainAxisSpacing: 6,
        crossAxisSpacing: 6,
        padding: const EdgeInsets.all(6),
        itemCount: posts.length + (provider.hasMore ? 1 : 0),
        itemBuilder: (context, index) {
          if (index == posts.length) {
            return const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(child: CircularProgressIndicator()),
            );
          }
          final post = posts[index];
          return PostGridItem(
            post: post,
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => DetailScreen(
                  posts: posts,
                  initialIndex: index,
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildSuggestionOverlay(BuildContext context) {
    return Positioned(
      top: 0,
      left: 0,
      right: 0,
      child: Material(
        elevation: 4,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxHeight: 220),
          child: _isLoadingSuggestions
              ? const Padding(
                  padding: EdgeInsets.all(16),
                  child: Center(child: CircularProgressIndicator()),
                )
              : ListView.builder(
                  shrinkWrap: true,
                  itemCount: _suggestions.length,
                  itemBuilder: (context, i) {
                    final tag = _suggestions[i];
                    return ListTile(
                      dense: true,
                      leading: _tagTypeIcon(tag.type),
                      title: Text(
                        tag.name.replaceAll('_', ' '),
                        style: const TextStyle(fontSize: 13),
                      ),
                      trailing: Text(
                        tag.count.toString(),
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                      onTap: () => _selectSuggestion(tag.name),
                    );
                  },
                ),
        ),
      ),
    );
  }

  Widget _tagTypeIcon(int type) {
    final color = switch (type) {
      TagType.artist => Colors.red.shade300,
      TagType.copyright => Colors.purple.shade300,
      TagType.character => Colors.green.shade300,
      _ => Colors.grey,
    };
    return Icon(Icons.label, color: color, size: 18);
  }
}
