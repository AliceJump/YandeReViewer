import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'providers/blacklist_provider.dart';
import 'providers/favorite_tags_provider.dart';
import 'providers/favorites_provider.dart';
import 'providers/post_provider.dart';
import 'screens/home_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Load persisted data before app start.
  final favoritesProvider = FavoritesProvider();
  final blacklistProvider = BlacklistProvider();
  final favoriteTagsProvider = FavoriteTagsProvider();

  await Future.wait([
    favoritesProvider.load(),
    blacklistProvider.load(),
    favoriteTagsProvider.load(),
  ]);

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: favoritesProvider),
        ChangeNotifierProvider.value(value: blacklistProvider),
        ChangeNotifierProvider.value(value: favoriteTagsProvider),
        ChangeNotifierProvider(
          create: (ctx) => PostProvider(
            blacklistProvider: ctx.read<BlacklistProvider>(),
            favoritesProvider: ctx.read<FavoritesProvider>(),
          ),
        ),
      ],
      child: const YandeViewerApp(),
    ),
  );
}

class YandeViewerApp extends StatelessWidget {
  const YandeViewerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'YandeReViewer',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF5B6CF6),
          brightness: Brightness.light,
        ),
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF5B6CF6),
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
      themeMode: ThemeMode.system,
      home: const HomeScreen(),
    );
  }
}
