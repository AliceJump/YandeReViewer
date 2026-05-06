import 'package:dio/dio.dart';
import '../models/post.dart';
import '../models/tag_info.dart';

class YandeApi {
  YandeApi._();
  static final YandeApi instance = YandeApi._();

  final Dio _dio = Dio(
    BaseOptions(
      baseUrl: 'https://yande.re/',
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 30),
      headers: {
        'User-Agent': 'YandeReViewer-Flutter/1.0',
      },
    ),
  );

  /// Fetch a page of posts. [tags] can contain multiple space-separated tags.
  Future<List<Post>> getPosts({
    String tags = '',
    int page = 1,
    int limit = 20,
  }) async {
    final response = await _dio.get<List<dynamic>>(
      'post.json',
      queryParameters: {
        'tags': tags,
        'limit': limit,
        'page': page,
      },
    );
    final list = response.data ?? [];
    return list
        .map((e) => Post.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Search tags by prefix for autocomplete.
  Future<List<TagInfo>> searchTags(String name, {int limit = 20}) async {
    final response = await _dio.get<List<dynamic>>(
      'tag.json',
      queryParameters: {
        'name': name,
        'limit': limit,
        'order': 'count',
      },
    );
    final list = response.data ?? [];
    return list
        .map((e) => TagInfo.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
