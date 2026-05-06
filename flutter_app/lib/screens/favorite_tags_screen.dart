import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/yande_api.dart';
import '../models/tag_info.dart';
import '../providers/favorite_tags_provider.dart';

class FavoriteTagsScreen extends StatefulWidget {
  const FavoriteTagsScreen({super.key});

  @override
  State<FavoriteTagsScreen> createState() => _FavoriteTagsScreenState();
}

class _FavoriteTagsScreenState extends State<FavoriteTagsScreen> {
  final TextEditingController _controller = TextEditingController();
  List<TagInfo> _suggestions = [];
  bool _isSearching = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _fetchSuggestions(String text) async {
    final trimmed = text.trim();
    if (trimmed.isEmpty) {
      setState(() => _suggestions = []);
      return;
    }
    setState(() => _isSearching = true);
    try {
      final results = await YandeApi.instance.searchTags(trimmed, limit: 10);
      if (mounted) {
        setState(() {
          _suggestions = results;
          _isSearching = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isSearching = false);
    }
  }

  void _addTag(String tag) {
    final trimmed = tag.trim();
    if (trimmed.isEmpty) return;
    context.read<FavoriteTagsProvider>().add(trimmed);
    _controller.clear();
    setState(() => _suggestions = []);
  }

  @override
  Widget build(BuildContext context) {
    final favTags = context.watch<FavoriteTagsProvider>().tags;

    return Scaffold(
      appBar: AppBar(title: const Text('收藏标签管理')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _controller,
                    decoration: const InputDecoration(
                      hintText: '搜索并添加标签...',
                      border: OutlineInputBorder(),
                      isDense: true,
                    ),
                    onChanged: _fetchSuggestions,
                    textInputAction: TextInputAction.done,
                    onSubmitted: _addTag,
                  ),
                ),
                const SizedBox(width: 8),
                FilledButton(
                  onPressed: () => _addTag(_controller.text),
                  child: const Text('添加'),
                ),
              ],
            ),
          ),

          // Suggestions dropdown
          if (_isSearching)
            const Padding(
              padding: EdgeInsets.all(8),
              child: CircularProgressIndicator(),
            )
          else if (_suggestions.isNotEmpty)
            Container(
              constraints: const BoxConstraints(maxHeight: 200),
              margin: const EdgeInsets.symmetric(horizontal: 12),
              decoration: BoxDecoration(
                border: Border.all(
                  color: Theme.of(context).colorScheme.outline,
                ),
                borderRadius: BorderRadius.circular(8),
              ),
              child: ListView.builder(
                shrinkWrap: true,
                itemCount: _suggestions.length,
                itemBuilder: (context, i) {
                  final tag = _suggestions[i];
                  return ListTile(
                    dense: true,
                    title: Text(tag.name.replaceAll('_', ' ')),
                    trailing: Text(
                      tag.count.toString(),
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    onTap: () => _addTag(tag.name),
                  );
                },
              ),
            ),

          const Divider(),

          if (favTags.isEmpty)
            const Expanded(
              child: Center(child: Text('暂无收藏标签')),
            )
          else
            Expanded(
              child: ListView.builder(
                itemCount: favTags.length,
                itemBuilder: (context, i) {
                  final tag = favTags[i];
                  return ListTile(
                    leading: const Icon(Icons.label_outline),
                    title: Text(tag.replaceAll('_', ' ')),
                    trailing: IconButton(
                      icon: const Icon(Icons.remove_circle_outline),
                      onPressed: () =>
                          context.read<FavoriteTagsProvider>().remove(tag),
                    ),
                  );
                },
              ),
            ),
        ],
      ),
    );
  }
}
