<?xml version="1.0"?>
<recipe>
    <merge from="AndroidManifest.xml.ftl" />

    <copy from="res/xml" />

    <instantiate from="res/values/strings.xml.ftl"
                   to="res/values/strings_${simpleName}.xml" />

    <instantiate from="src/app_package/SettingsActivity.java.ftl"
                   to="${srcOut}/${activityClass}.java" />

    <open file="${srcOut}/${activityClass}.java" />
</recipe>
