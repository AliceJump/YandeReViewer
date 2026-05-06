/// tag.type values from yande.re API:
///   0 = general
///   1 = artist
///   3 = copyright
///   4 = character
class TagInfo {
  final int id;
  final String name;
  final int type;
  final int count;
  final bool ambiguous;

  const TagInfo({
    required this.id,
    required this.name,
    required this.type,
    required this.count,
    this.ambiguous = false,
  });

  factory TagInfo.fromJson(Map<String, dynamic> json) => TagInfo(
        id: (json['id'] as num).toInt(),
        name: json['name'] as String? ?? '',
        type: (json['type'] as num?)?.toInt() ?? 0,
        count: (json['count'] as num?)?.toInt() ?? 0,
        ambiguous: json['ambiguous'] == true || json['ambiguous'] == 1,
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'type': type,
        'count': count,
        'ambiguous': ambiguous,
      };
}
