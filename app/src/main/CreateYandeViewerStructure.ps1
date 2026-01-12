# 布局模板内容
$layoutTemplates = @{
    "res\layout\activity_main.xml" = @"
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <EditText
        android:id="@+id/searchBox"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:hint="Search"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toStartOf="@+id/searchBtn"
        android:layout_margin="8dp"/>

    <Button
        android:id="@+id/searchBtn"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Search"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_margin="8dp"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/searchBox"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"/>
</androidx.constraintlayout.widget.ConstraintLayout>
"@

    "res\layout\activity_detail.xml" = @"
<?xml version=\"1.0\" encoding=\"utf-8\"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android=\"http://schemas.android.com/apk/res/android\"
    xmlns:app=\"http://schemas.android.com/apk/res-auto\"
    android:layout_width=\"match_parent\"
    android:layout_height=\"match_parent\">

    <ImageView
        android:id=\"@+id/detailImage\"
        android:layout_width=\"0dp\"
        android:layout_height=\"0dp\"
        android:scaleType=\"fitCenter\"
        app:layout_constraintTop_toTopOf=\"parent\"
        app:layout_constraintStart_toStartOf=\"parent\"
        app:layout_constraintEnd_toEndOf=\"parent\"
        app:layout_constraintBottom_toBottomOf=\"parent\"/>

</androidx.constraintlayout.widget.ConstraintLayout>
"@

    "res\layout\item_post.xml" = @"
<?xml version=\"1.0\" encoding=\"utf-8\"?>
<androidx.cardview.widget.CardView
    xmlns:android=\"http://schemas.android.com/apk/res/android\"
    xmlns:app=\"http://schemas.android.com/apk/res-auto\"
    android:layout_width=\"match_parent\"
    android:layout_height=\"wrap_content\"
    android:layout_margin=\"4dp\"
    app:cardCornerRadius=\"8dp\"
    app:cardElevation=\"4dp\">

    <ImageView
        android:id=\"@+id/postImage\"
        android:layout_width=\"match_parent\"
        android:layout_height=\"150dp\"
        android:scaleType=\"centerCrop\"/>

</androidx.cardview.widget.CardView>
"@
}

# 创建布局文件并写入模板
foreach ($layout in $layoutTemplates.Keys) {
    $fullPath = Join-Path $RootPath $layout
    if (-not (Test-Path $fullPath)) {
        $layoutTemplates[$layout] | Out-File -FilePath $fullPath -Encoding UTF8
        Write-Host "Created layout: $fullPath"
    }
}
