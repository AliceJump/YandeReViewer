import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/blacklist_provider.dart';

class BlacklistScreen extends StatefulWidget {
  const BlacklistScreen({super.key});

  @override
  State<BlacklistScreen> createState() => _BlacklistScreenState();
}

class _BlacklistScreenState extends State<BlacklistScreen> {
  final TextEditingController _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _addTag(String tag) {
    final trimmed = tag.trim();
    if (trimmed.isEmpty) return;
    context.read<BlacklistProvider>().add(trimmed);
    _controller.clear();
  }

  @override
  Widget build(BuildContext context) {
    final blacklist = context.watch<BlacklistProvider>().blacklist.toList()
      ..sort();

    return Scaffold(
      appBar: AppBar(
        title: const Text('黑名单管理'),
        actions: [
          if (blacklist.isNotEmpty)
            IconButton(
              icon: const Icon(Icons.delete_sweep),
              tooltip: '清空黑名单',
              onPressed: () => _confirmClear(context),
            ),
        ],
      ),
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
                      hintText: '添加黑名单标签...',
                      border: OutlineInputBorder(),
                      isDense: true,
                    ),
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
          const Divider(height: 1),
          if (blacklist.isEmpty)
            const Expanded(
              child: Center(child: Text('黑名单为空')),
            )
          else
            Expanded(
              child: ListView.builder(
                itemCount: blacklist.length,
                itemBuilder: (context, i) {
                  final tag = blacklist[i];
                  return ListTile(
                    leading: const Icon(Icons.block),
                    title: Text(tag.replaceAll('_', ' ')),
                    trailing: IconButton(
                      icon: const Icon(Icons.remove_circle_outline),
                      onPressed: () =>
                          context.read<BlacklistProvider>().remove(tag),
                    ),
                  );
                },
              ),
            ),
        ],
      ),
    );
  }

  Future<void> _confirmClear(BuildContext context) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('清空黑名单'),
        content: const Text('确定要清空所有黑名单标签吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('清空'),
          ),
        ],
      ),
    );
    if (confirm == true && mounted) {
      context.read<BlacklistProvider>().clear();
    }
  }
}
