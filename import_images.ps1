$sourceDir = "c:\Users\mukht\Desktop\Android\Kenyatourism-master\Destination images"
$destDir = "c:\Users\mukht\Desktop\Android\Kenyatourism-master\app\src\main\res\drawable"

Get-ChildItem -Path $sourceDir | ForEach-Object {
    $name = $_.Name.ToLower()
    # Replace non-alphanumeric (except dot) with underscore
    $name = $name -replace '[^a-z0-9.]', '_'
    # Remove duplicate underscores
    $name = $name -replace '_+', '_'
    # Remove leading/trailing underscores (basename only)
    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($name).Trim('_')
    $ext = [System.IO.Path]::GetExtension($name)
    $finalName = $baseName + $ext

    # Check for valid start (must start with letter, but we can prepend 'img_' if needed, mostly destinations start with letters)
    if ($finalName -match "^[0-9]") {
        $finalName = "img_" + $finalName
    }

    Copy-Item $_.FullName -Destination (Join-Path $destDir $finalName)
    Write-Host "Copied $finalName"
}
