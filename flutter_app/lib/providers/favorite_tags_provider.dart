import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

class FavoriteTagsProvider extends ChangeNotifier {
  static const _prefKey = 'favorite_tags_list';

  List<String> _tags = [];

  List<String> get tags => List.unmodifiable(_tags);

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final json = prefs.getString(_prefKey);
    if (json != null && json.isNotEmpty) {
      try {
        _tags = (jsonDecode(json) as List<dynamic>).cast<String>();
      } catch (_) {
        _tags = [];
      }
    }
    notifyListeners();
  }

  Future<void> _save() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefKey, jsonEncode(_tags));
  }

  bool contains(String tag) => _tags.contains(tag);

  Future<void> add(String tag) async {
    if (!_tags.contains(tag)) {
      _tags.add(tag);
      notifyListeners();
      await _save();
    }
  }

  Future<void> remove(String tag) async {
    if (_tags.remove(tag)) {
      notifyListeners();
      await _save();
    }
  }
}
