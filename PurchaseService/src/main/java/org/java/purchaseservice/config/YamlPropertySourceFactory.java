package org.java.purchaseservice.config;

import java.io.IOException;
import java.util.List;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/** Custom PropertySource factory for loading YAML files with @PropertySource annotation. */
public class YamlPropertySourceFactory implements PropertySourceFactory {

  /** Creates PropertySource from YAML resource using YamlPropertySourceLoader. */
  @Override
  @NonNull
  public PropertySource<?> createPropertySource(
      @Nullable String name, @NonNull EncodedResource resource) throws IOException {
    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    List<PropertySource<?>> sources =
        loader.load(
            name != null ? name : resource.getResource().getFilename(), resource.getResource());

    if (sources.isEmpty()) {
      throw new IllegalArgumentException(
          "No property sources found in " + resource.getResource().getFilename());
    }

    return sources.get(0);
  }
}
