/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.configurations;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.artifacts.ConfigurablePublishArtifact;
import org.gradle.api.artifacts.ConfigurationPublications;
import org.gradle.api.artifacts.ConfigurationVariant;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.PublishArtifactSet;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.internal.FactoryNamedDomainObjectContainer;
import org.gradle.api.internal.attributes.AttributeContainerInternal;
import org.gradle.api.internal.attributes.DefaultMutableAttributeContainer;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.typeconversion.NotationParser;

import java.util.LinkedHashSet;
import java.util.Set;

public class DefaultConfigurationPublications implements ConfigurationPublications {
    private final PublishArtifactSet artifacts;
    private final PublishArtifactSet allArtifacts;
    private final AttributeContainerInternal parentAttributes;
    private final AttributeContainerInternal attributes;
    private final Instantiator instantiator;
    private final NotationParser<Object, ConfigurablePublishArtifact> artifactNotationParser;
    private final FileCollectionFactory fileCollectionFactory;
    private final ImmutableAttributesFactory attributesFactory;
    private FactoryNamedDomainObjectContainer<ConfigurationVariant> variants;

    public DefaultConfigurationPublications(PublishArtifactSet artifacts, PublishArtifactSet allArtifacts, AttributeContainerInternal parentAttributes, Instantiator instantiator, NotationParser<Object, ConfigurablePublishArtifact> artifactNotationParser, FileCollectionFactory fileCollectionFactory, ImmutableAttributesFactory attributesFactory) {
        this.artifacts = artifacts;
        this.allArtifacts = allArtifacts;
        this.parentAttributes = parentAttributes;
        this.instantiator = instantiator;
        this.artifactNotationParser = artifactNotationParser;
        this.fileCollectionFactory = fileCollectionFactory;
        this.attributesFactory = attributesFactory;
        this.attributes = new DefaultMutableAttributeContainer(attributesFactory, parentAttributes);
    }

    public OutgoingVariant convertToOutgoingVariant() {
        return new OutgoingVariant() {
            @Override
            public AttributeContainerInternal getAttributes() {
                return attributes;
            }

            @Override
            public Set<? extends PublishArtifact> getArtifacts() {
                return artifacts;
            }

            @Override
            public Set<? extends OutgoingVariant> getChildren() {
                Set<OutgoingVariant> result = new LinkedHashSet<OutgoingVariant>();
                if (allArtifacts.size() > 0 || variants == null) {
                    result.add(new LeafOutgoingVariant(attributes, allArtifacts));
                }
                if (variants != null) {
                    for (DefaultVariant variant : variants.withType(DefaultVariant.class)) {
                        result.add(variant.convertToOutgoingVariant());
                    }
                }
                return result;
            }
        };
    }

    @Override
    public AttributeContainer getAttributes() {
        return attributes;
    }

    @Override
    public ConfigurationPublications attributes(Action<? super AttributeContainer> action) {
        action.execute(attributes);
        return this;
    }

    @Override
    public PublishArtifactSet getArtifacts() {
        return artifacts;
    }

    @Override
    public void artifact(Object notation) {
        artifacts.add(artifactNotationParser.parseNotation(notation));
    }

    @Override
    public void artifact(Object notation, Action<? super ConfigurablePublishArtifact> configureAction) {
        ConfigurablePublishArtifact publishArtifact = artifactNotationParser.parseNotation(notation);
        artifacts.add(publishArtifact);
        configureAction.execute(publishArtifact);
    }

    @Override
    public NamedDomainObjectContainer<ConfigurationVariant> getVariants() {
        if (variants == null) {
            // Create variants container only as required
            variants = new FactoryNamedDomainObjectContainer<ConfigurationVariant>(ConfigurationVariant.class, instantiator, new NamedDomainObjectFactory<ConfigurationVariant>() {
                @Override
                public ConfigurationVariant create(String name) {
                    return instantiator.newInstance(DefaultVariant.class, name, parentAttributes, artifactNotationParser, fileCollectionFactory, attributesFactory);
                }
            });
        }
        return variants;
    }

    @Override
    public void variants(Action<? super NamedDomainObjectContainer<ConfigurationVariant>> configureAction) {
        configureAction.execute(getVariants());
    }

}
