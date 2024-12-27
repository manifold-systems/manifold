package manifold.api.properties;

import static manifold.api.properties.ResourceBundleFiles.ResourceBundleProperties.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import manifold.api.fs.IFile;

public class ResourceBundleFiles {
    private static final Pattern RESOURCE_BUNDLE_PATTERN = Pattern.compile(
        String.format("^(?<%s>[a-zA-Z0-9_]+?)(_(?<%s>[a-zA-Z]{2,3})(_(?<%s>[a-zA-Z]{2})(_(?<%s>[a-zA-Z0-9_]+))?)?)?$",
            BUNDLE_NAME, LANGUAGE, COUNTRY, VARIANT));
    private final Map<String, Map<String, Map<String, Map<String, Map<String, IFile>>>>> resourceBundleFileMap =
        new HashMap<>();

    public void addFile(IFile file, String fqn) {
        String bundelName = file.getBaseName();
        Matcher matcher = RESOURCE_BUNDLE_PATTERN.matcher(bundelName);
        if (matcher.matches()) {
            FqnBundle fqnBundle = new FqnBundle(fqn);
            resourceBundleFileMap.computeIfAbsent(fqnBundle.parentFqn, k -> new HashMap<>())
                .computeIfAbsent(fqnBundle.bundleName, k -> new HashMap<>())
                .computeIfAbsent(fqnBundle.language, k -> new HashMap<>())
                .computeIfAbsent(fqnBundle.country, k -> new HashMap<>())
                .put(fqnBundle.variant, file);
        }
    }

    public void removeSingleFileResourceBundles() {
        resourceBundleFileMap.forEach((k, v) -> v.entrySet().removeIf(entry -> entry.getValue().size() == 1));
        resourceBundleFileMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public Type getType(String topLevelFqn) {
        FqnBundle fqnBundle = new FqnBundle(topLevelFqn);
        Map<String, Map<String, Map<String, Map<String, IFile>>>> bundleMap =
            resourceBundleFileMap.get(fqnBundle.getParentFqn());
        if (bundleMap == null) {
            return Type.NONE;
        }
        Map<String, Map<String, Map<String, IFile>>> languageMap =
            bundleMap.get(fqnBundle.getBundleName());
        if (languageMap == null) {
            return Type.NONE;
        }
        Map<String, Map<String, IFile>> countryMap = languageMap.get(fqnBundle.getLanguage());
        if (countryMap == null) {
            return Type.NONE;
        }
        Map<String, IFile> variantMap = countryMap.get(fqnBundle.getCountry());
        if (variantMap == null) {
            return Type.NONE;
        }
        IFile file = variantMap.get(fqnBundle.getVariant());
        if( file == null ){
            return Type.NONE;
        }
        return fqnBundle.isSpecific() ? Type.SPECIFIC : Type.DEFAULT;
    }

    public enum Type {
        DEFAULT, SPECIFIC, NONE;
    }

    private static class FqnBundle {
        private final String parentFqn;
        private final String bundleName;
        private final String language;
        private final String country;
        private final String variant;

        FqnBundle(String fqn) {
            String name;
            if (fqn.contains(".")) {
                int lastIndexOfDot = fqn.lastIndexOf('.');
                parentFqn = fqn.substring(0, lastIndexOfDot);
                name = fqn.substring(lastIndexOfDot + 1);
            } else {
                parentFqn = "";
                name = fqn;
            }
            Matcher matcher = RESOURCE_BUNDLE_PATTERN.matcher(name);
            if(matcher.find()) {
                bundleName = matcher.group(BUNDLE_NAME.getValue());
                language = matcher.group(LANGUAGE.getValue());
                country = matcher.group(COUNTRY.getValue());
                variant = matcher.group(VARIANT.getValue());
            }else{
                bundleName = null;
                language = null;
                country = null;
                variant = null;
            }
        }

        public boolean isSpecific(){
            return language != null;
        }

        public String getParentFqn() {
            return parentFqn;
        }

        public String getBundleName() {
            return bundleName;
        }

        public String getLanguage() {
            return language;
        }

        public String getCountry() {
            return country;
        }

        public String getVariant() {
            return variant;
        }
    }

    enum ResourceBundleProperties {
        BUNDLE_NAME("bundlename"),
        LANGUAGE("language"),
        COUNTRY("country"),
        VARIANT("variant");

        private final String value;

        ResourceBundleProperties(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
