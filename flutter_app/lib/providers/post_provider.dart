import 'package:flutter/foundation.dart';
import '../api/yande_api.dart';
import '../models/post.dart';
import 'blacklist_provider.dart';
import 'favorites_provider.dart';

enum BrowseMode { normal, favorites }

class PostProvider extends ChangeNotifier {
  final BlacklistProvider blacklistProvider;
  final FavoritesProvider favoritesProvider;

  PostProvider({
    required this.blacklistProvider,
    required this.favoritesProvider,
  });

  // ── search state ──────────────────────────────────────────────
  String _query = '';
  final Set<String> _ratings = {'s', 'q', 'e'};
  BrowseMode _mode = BrowseMode.normal;

  String get query => _query;
  Set<String> get ratings => Set.unmodifiable(_ratings);
  BrowseMode get mode => _mode;

  // ── page state ────────────────────────────────────────────────
  final List<Post> _posts = [];
  int _page = 1;
  bool _isLoading = false;
  bool _hasMore = true;
  String? _error;

  List<Post> get posts => List.unmodifiable(_posts);
  bool get isLoading => _isLoading;
  bool get hasMore => _hasMore;
  String? get error => _error;

  // ── public API ────────────────────────────────────────────────

  void setMode(BrowseMode mode) {
    if (_mode == mode) return;
    _mode = mode;
    refresh();
  }

  void toggleRating(String rating) {
    if (_ratings.contains(rating)) {
      if (_ratings.length == 1) return; // keep at least one
      _ratings.remove(rating);
    } else {
      _ratings.add(rating);
    }
    refresh();
  }

  void search(String query) {
    _query = query.trim();
    refresh();
  }

  void refresh() {
    _posts.clear();
    _page = 1;
    _hasMore = true;
    _error = null;
    notifyListeners();
    loadMore();
  }

  Future<void> loadMore() async {
    if (_isLoading || !_hasMore) return;
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final newPosts = await _fetchPage(_page);
      _posts.addAll(newPosts);
      _hasMore = newPosts.isNotEmpty;
      if (_hasMore) _page++;
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<List<Post>> _fetchPage(int page) async {
    if (_mode == BrowseMode.favorites) {
      // For favorites we load all at once; only call this on page 1.
      if (page > 1) return [];
      final tags = _query.split(' ').where((t) => t.isNotEmpty).toList();
      return favoritesProvider.filter(tags, _ratings);
    }

    // Build effective tags query including rating filters.
    final ratingPart = _ratings.length < 3
        ? _ratings.map((r) => 'rating:$r').join(' ')
        : '';
    final effectiveQuery = [_query, ratingPart]
        .where((s) => s.isNotEmpty)
        .join(' ');

    final fetched = await YandeApi.instance
        .getPosts(tags: effectiveQuery, page: page);

    // Apply blacklist filter.
    final blacklist = blacklistProvider.blacklist;
    return fetched.where((p) {
      if (blacklist.isEmpty) return true;
      final postTags = p.tagList.toSet();
      return postTags.intersection(blacklist).isEmpty;
    }).toList();
  }
}
