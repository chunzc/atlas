package org.openstreetmap.atlas.tags.annotations.validation;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.openstreetmap.atlas.tags.annotations.TagKey;
import org.openstreetmap.atlas.tags.annotations.validation.Validators.TagKeySearch;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

/**
 * Class that walks across all Tags and generates metadata about them that can be converted into
 * HTML or any other desired documentation format as needed.
 *
 * @author cstaylor
 */
public class TagDocumenter
{
    /**
     * Callback that receives metadata about a Tag
     *
     * @author cstaylor
     */
    public interface Callback
    {
        void tagFound(CallbackData data);
    }

    /**
     * Metadata container class for Tag information
     *
     * @author cstaylor
     */
    public static final class CallbackData implements Comparable<CallbackData>
    {
        private String tagClassName;
        private String tagKey;
        private final SortedSet<String> validTagValues;
        private URI tagInfoLink;
        private URI osmWikiLink;
        private String validationType;
        private boolean localized;
        private boolean synthetic;

        CallbackData()
        {
            this.validTagValues = new TreeSet<>();
        }

        @Override
        public int compareTo(final CallbackData other)
        {
            return this.tagKey.compareTo(other.tagKey);
        }

        @Override
        public boolean equals(final Object obj)
        {
            if (obj == this)
            {
                return true;
            }
            if (obj instanceof CallbackData)
            {
                final CallbackData other = (CallbackData) obj;
                boolean returnValue = Objects.equals(this.tagClassName, other.tagClassName);
                returnValue = returnValue && Objects.equals(this.tagKey, other.tagKey);
                returnValue = returnValue
                        && Objects.equals(this.validTagValues, other.validTagValues);
                returnValue = returnValue && Objects.equals(this.tagInfoLink, other.tagInfoLink);
                returnValue = returnValue && Objects.equals(this.osmWikiLink, other.osmWikiLink);
                returnValue = returnValue
                        && Objects.equals(this.validationType, other.validationType);
                returnValue = returnValue && Objects.equals(this.localized, other.localized);
                returnValue = returnValue && Objects.equals(this.synthetic, other.synthetic);
                return returnValue;
            }
            return false;
        }

        public Optional<URI> getOsmWikiLink()
        {
            return Optional.ofNullable(this.osmWikiLink);
        }

        public String getTagClassName()
        {
            return this.tagClassName;
        }

        public Optional<URI> getTagInfoLink()
        {
            return Optional.ofNullable(this.tagInfoLink);
        }

        public String getTagKey()
        {
            return this.tagKey;
        }

        public Iterable<String> getValidTagValues()
        {
            return this.validTagValues;
        }

        public String getValidationType()
        {
            return this.validationType;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(this.tagKey, this.tagClassName);
        }

        public boolean isLocalized()
        {
            return this.localized;
        }

        public boolean isSynthetic()
        {
            return this.synthetic;
        }
    }

    private final Set<CallbackData> tagData;

    /**
     * Calls the other constructor with the default package name
     */
    public TagDocumenter()
    {
        this("org.openstreetmap.atlas");
    }

    /**
     * Find all of the tags in packageName and create metadata for all of them
     *
     * @param packageName
     *            the base package to search the current classloader for Tags
     */
    public TagDocumenter(final String packageName)
    {
        this.tagData = new TreeSet<>();
        /*
         * We definitely don't want tags in classes named TestCase. When running this from the
         * command line we shouldn't get any TestCase tags anyways, but when running this in
         * development mode under Eclipse with core in the classpath they will be picked up.
         */

        // Scan the given package
        try (ScanResult scanResult = new ClassGraph().enableAllInfo().whitelistPackages(packageName)
                .scan())
        {
            // Look at annotated classes
            final ClassInfoList tagClassInfoList = scanResult
                    .getClassesWithAnnotation("org.openstreetmap.atlas.tags.annotations.Tag");

            // Ignore any TestCase classes
            tagClassInfoList.loadClasses().forEach(klass ->
            {
                if (!klass.getName().contains("TestCase"))
                {
                    this.tagData.add(createCallbackDataFromClass(klass));
                }
            });
        }
    }

    public void walk(final Callback callback)
    {
        this.tagData.stream().forEach(callback::tagFound);
    }

    private CallbackData createCallbackDataFromClass(final Class<?> tagClass)
    {
        final CallbackData returnValue = new CallbackData();
        TagKeySearch.findTagKeyIn(tagClass).ifPresent(results ->
        {
            final String tagName = results.getKeyName();
            final TagKey tagKey = results.getTagKey();
            returnValue.tagKey = tagName;
            returnValue.tagClassName = tagClass.getName();
            returnValue.osmWikiLink = results.getTag().osm().length() > 0
                    ? URI.create(results.getTag().osm())
                    : null;
            returnValue.tagInfoLink = results.getTag().taginfo().length() > 0
                    ? URI.create(results.getTag().taginfo())
                    : null;
            returnValue.localized = tagKey.value() == TagKey.KeyType.LOCALIZED;
            returnValue.validationType = results.getTag().value().name();
            returnValue.synthetic = results.getTag().synthetic();
        });
        return returnValue;
    }
}
