@echo off
REM ──────────────────────────────────────────────────────────────────────────────
REM build_and_run.bat  –  Uses JavaFX 21.0.6 jars from Maven local repository
REM ──────────────────────────────────────────────────────────────────────────────

SET FX=C:\Users\Egypt\.m2\repository\org\openjfx
SET FX_VER=21.0.6

SET FX_MODS=%FX%\javafx-base\%FX_VER%\javafx-base-%FX_VER%-win.jar;%FX%\javafx-controls\%FX_VER%\javafx-controls-%FX_VER%-win.jar;%FX%\javafx-fxml\%FX_VER%\javafx-fxml-%FX_VER%-win.jar;%FX%\javafx-graphics\%FX_VER%\javafx-graphics-%FX_VER%-win.jar

SET SRC_DIR=%~dp0

mkdir out 2>nul

echo Compiling...
javac --module-path "%FX_MODS%" ^
      --add-modules javafx.controls,javafx.fxml ^
      -d out ^
      "%SRC_DIR%Sort.java" ^
      "%SRC_DIR%BubbleSort.java" ^
      "%SRC_DIR%SelectionSort.java" ^
      "%SRC_DIR%InsertionSort.java" ^
      "%SRC_DIR%MergeSort.java" ^
      "%SRC_DIR%HeapSort.java" ^
      "%SRC_DIR%QuickSort.java" ^
      "%SRC_DIR%Generator.java" ^
      "%SRC_DIR%InputReader.java" ^
      "%SRC_DIR%MainApp.java"

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo Compilation FAILED. Check errors above.
    pause
    exit /b 1
)

echo.
echo Compilation successful! Launching app...
java --module-path "%FX_MODS%;out" ^
     --add-modules javafx.controls,javafx.fxml ^
     --add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED ^
     --enable-native-access=javafx.graphics ^
     -cp out MainApp

pause
