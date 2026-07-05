import re

path = "/app/applet/app/src/main/java/com/example/ui/screens/SettingsScreen.kt"

with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Pattern 1: .clip(RoundedCornerShape(X.dp))\n\s*\.background(glassFill)\n\s*\.border(1.dp, glassBorder, RoundedCornerShape(X.dp))
pattern1 = r"\.clip\(RoundedCornerShape\(([^)]+)\)\)\s*\n\s*\.background\(glassFill\)\s*\n\s*\.border\(1\.dp,\s*glassBorder,\s*RoundedCornerShape\(\1\)\)"

# We replace it with .premiumGlassBg(cornerRadius = \1)
replaced1, count1 = re.subn(pattern1, r".premiumGlassBg(cornerRadius = \1)", content)

# Pattern 2: .background(glassFill)\n\s*\.border(1.dp, glassBorder)
pattern2 = r"\.background\(glassFill\)\s*\n\s*\.border\(1\.dp,\s*glassBorder\)"
replaced2, count2 = re.subn(pattern2, r".premiumGlassBg(cornerRadius = 0.dp, borderWidth = 1.dp, showSpecularHighlight = false)", replaced1)

# Pattern 3: .background(glassFill) alone
pattern3 = r"\.background\(glassFill\)"
replaced3, count3 = re.subn(pattern3, r".premiumGlassBg(cornerRadius = 0.dp, borderWidth = 0.dp, showSpecularHighlight = false)", replaced2)

print(f"Replaced {count1} rounded glass cards.")
print(f"Replaced {count2} glass headers.")
print(f"Replaced {count3} other glass fills.")

with open(path, "w", encoding="utf-8") as f:
    f.write(replaced3)
