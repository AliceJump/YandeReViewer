import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/post.dart';

class FavoritesProvider extends ChangeNotifier {
  static const _prefKey = 'favorite_posts';

  List<Post> _favorites = [];

  List<Post> get favorites => List.unmodifiable(_favorites);

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final json = prefs.getString(_prefKey);
    if (json != null && json.isNotEmpty) {
      try {
        _favorites = Post.listFromJson(json);
      } catch (_) {
        _favorites = [];
      }
    }
    notifyListeners();
  }

  Future<void> _save() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefKey, Post.listToJson(_favorites));
  }

  bool isFavorite(int postId) => _favorites.any((p) => p.id == postId);

  Future<void> add(Post post) async {
    _favorites.removeWhere((p) => p.id == post.id);
    _favorites.add(post.copyWith(
      favoriteAt: DateTime.now().millisecondsSinceEpoch,
    ));
    notifyListeners();
    await _save();
  }

  Future<void> remove(int postId) async {
    _favorites.removeWhere((p) => p.id == postId);
    notifyListeners();
    await _save();
  }

  /// Filter favorites by query tags and allowed ratings.
  List<Post> filter(List<String> queryTags, Set<String> ratings) {
    return _favorites
        .where((p) => ratings.contains(p.rating))
        .where((p) {
          if (queryTags.isEmpty) return true;
          final postTags = p.tagList.toSet();
          return queryTags.every((q) {
            if (q.startsWith('rating:')) {
              return p.rating == q.replaceFirst('rating:', '');
            }
            return postTags.contains(q);
          });
        })
        .toList()
      ..sort((a, b) => b.favoriteAt.compareTo(a.favoriteAt));
  }
}
