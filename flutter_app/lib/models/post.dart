import 'dart:convert';

class Post {
  final int id;
  final String previewUrl;
  final String fileUrl;
  final String tags;
  final String? source;
  final int width;
  final int height;
  final String rating;
  final int favoriteAt;

  const Post({
    required this.id,
    required this.previewUrl,
    required this.fileUrl,
    required this.tags,
    this.source,
    required this.width,
    required this.height,
    required this.rating,
    this.favoriteAt = 0,
  });

  factory Post.fromJson(Map<String, dynamic> json) => Post(
        id: (json['id'] as num).toInt(),
        previewUrl: json['preview_url'] as String? ?? '',
        fileUrl: json['file_url'] as String? ?? '',
        tags: json['tags'] as String? ?? '',
        source: json['source'] as String?,
        width: (json['width'] as num?)?.toInt() ?? 0,
        height: (json['height'] as num?)?.toInt() ?? 0,
        rating: json['rating'] as String? ?? 's',
        favoriteAt: (json['favoriteAt'] as num?)?.toInt() ?? 0,
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'preview_url': previewUrl,
        'file_url': fileUrl,
        'tags': tags,
        'source': source,
        'width': width,
        'height': height,
        'rating': rating,
        'favoriteAt': favoriteAt,
      };

  Post copyWith({int? favoriteAt}) => Post(
        id: id,
        previewUrl: previewUrl,
        fileUrl: fileUrl,
        tags: tags,
        source: source,
        width: width,
        height: height,
        rating: rating,
        favoriteAt: favoriteAt ?? this.favoriteAt,
      );

  List<String> get tagList => tags.split(' ').where((t) => t.isNotEmpty).toList();

  static List<Post> listFromJson(String src) {
    final list = jsonDecode(src) as List<dynamic>;
    return list.map((e) => Post.fromJson(e as Map<String, dynamic>)).toList();
  }

  static String listToJson(List<Post> posts) =>
      jsonEncode(posts.map((p) => p.toJson()).toList());
}
