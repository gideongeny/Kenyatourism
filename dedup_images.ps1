$dir = "c:\Users\mukht\Desktop\Android\Kenyatourism-master\app\src\main\res\drawable"
$groups = Get-ChildItem -Path $dir | Group-Object { [System.IO.Path]::GetFileNameWithoutExtension($_.Name) }

foreach ($group in $groups) {
    if ($group.Count -gt 1) {
        Write-Host "Found duplicates for $($group.Name)"
        # Keep the first one, delete the rest
        for ($i = 1; $i -lt $group.Count; $i++) {
            Remove-Item $group.Group[$i].FullName
            Write-Host "Removed duplicate: $($group.Group[$i].Name)"
        }
    }
}
