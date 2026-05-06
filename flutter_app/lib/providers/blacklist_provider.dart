import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

class BlacklistProvider extends ChangeNotifier {
  static const _prefKey = 'blacklist_tags';

  Set<String> _blacklist = {};

  Set<String> get blacklist => Set.unmodifiable(_blacklist);

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final json = prefs.getString(_prefKey);
    if (json != null && json.isNotEmpty) {
      try {
        final list = (jsonDecode(json) as List<dynamic>).cast<String>();
        _blacklist = list.toSet();
      } catch (_) {
        _blacklist = {};
      }
    }
    notifyListeners();
  }

  Future<void> _save() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefKey, jsonEncode(_blacklist.toList()));
  }

  bool contains(String tag) => _blacklist.contains(tag);

  Future<void> add(String tag) async {
    if (_blacklist.add(tag)) {
      notifyListeners();
      await _save();
    }
  }

  Future<void> remove(String tag) async {
    if (_blacklist.remove(tag)) {
      notifyListeners();
      await _save();
    }
  }

  Future<void> clear() async {
    _blacklist.clear();
    notifyListeners();
    await _save();
  }
}
