{ pkgs, ... }:

{
  # Language toolchain to compile and run the explorer.
  languages.java.enable = true;
  languages.java.jdk.package = pkgs.temurin-bin-21;
  languages.clojure.enable = true;

  packages = [ pkgs.git ];

  enterShell = ''
    echo "alchery-explorer devenv"
    echo "  clj -M:run       # start the explorer (talks to ALCHERY_API_URL)"
  '';
}
