package skinemsya.vse.ru.app.predictions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EveningFrameCatalog {

    private final List<EveningFrame> frames;
    private final List<String> times;

    @Autowired
    public EveningFrameCatalog() {
        this(loadDefault());
    }

    EveningFrameCatalog(Loaded loaded) {
        this.frames = List.copyOf(loaded.frames());
        this.times = List.copyOf(loaded.times());
    }

    public List<EveningFrame> frames() {
        return frames;
    }

    public List<String> times() {
        return times;
    }

    static Loaded loadDefault() {
        try (InputStream in = EveningFrameCatalog.class.getResourceAsStream("/predictions/evening-frames.json")) {
            if (in == null) {
                throw new IllegalStateException("Evening prediction catalog is missing");
            }
            Loaded loaded = new ObjectMapper().readValue(in, Loaded.class);
            if (loaded.frames() == null
                    || loaded.frames().isEmpty()
                    || loaded.times() == null
                    || loaded.times().isEmpty()) {
                throw new IllegalStateException("Evening prediction catalog is empty");
            }
            return loaded;
        } catch (IOException ex) {
            throw new IllegalStateException("Evening prediction catalog cannot be read", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Loaded(List<String> times, List<EveningFrame> frames) {}
}
