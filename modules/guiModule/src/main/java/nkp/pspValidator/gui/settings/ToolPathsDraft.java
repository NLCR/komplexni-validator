package nkp.pspValidator.gui.settings;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.newvalidation.ExternalTools;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;

import java.io.File;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Cesty (adresáře) k externím nástrojům v tabulce nástrojů. Nově nastavená nebo zrušená cesta platí pro kontrolu
 * a validaci hned (předá se manažeru nástrojů, {@code ExternalUtilManager#setPath}), do {@code config.properties} se
 * ale zapíše podle režimu:
 * <ul>
 *     <li>hned ({@link #immediate}): dialog Externí nástroje při startu aplikace, který tlačítko Uložit nemá;</li>
 *     <li>až {@link #save()} ({@link #deferred}): okno Nastavení, cesta je do uložení neuloženou změnou a {@link #discard()}
 *     vrátí manažeru uloženou cestu.</li>
 * </ul>
 * Klíče nastavení jsou stejné jako dřív ({@link ExternalTools#configKey}). Bez JavaFX.
 */
public class ToolPathsDraft {

    /** Kam se platná cesta nástroje předá (v aplikaci {@code ExternalUtilManager::setPath}). */
    public interface PathTarget {
        void setPath(ExternalUtil util, File path);
    }

    private final ConfigurationManager config;
    private final PathTarget target;
    private final boolean immediate;
    /** cesty uložené v nastavení (null = nenastavena) */
    private final Map<ExternalUtil, File> saved = new EnumMap<>(ExternalUtil.class);
    /** platné cesty (v režimu hned shodné s uloženými) */
    private final Map<ExternalUtil, File> current = new EnumMap<>(ExternalUtil.class);

    private ToolPathsDraft(ConfigurationManager config, PathTarget target, boolean immediate) {
        this.config = config;
        this.target = target;
        this.immediate = immediate;
        for (ExternalUtil util : ExternalUtil.values()) {
            File path = config.getFileOrNull(ExternalTools.configKey(util));
            saved.put(util, path);
            current.put(util, path);
        }
    }

    /** Cesta se zapíše do nastavení hned po výběru nebo zrušení. */
    public static ToolPathsDraft immediate(ConfigurationManager config, PathTarget target) {
        return new ToolPathsDraft(config, target, true);
    }

    /** Cesta se zapíše do nastavení až {@link #save()}, do té doby je neuloženou změnou. */
    public static ToolPathsDraft deferred(ConfigurationManager config, PathTarget target) {
        return new ToolPathsDraft(config, target, false);
    }

    public boolean isImmediate() {
        return immediate;
    }

    /** Platná cesta nástroje (nastavená v tabulce, jinak uložená), null když není nastavena. */
    public File getPath(ExternalUtil util) {
        return current.get(util);
    }

    /**
     * Nastaví cestu nástroje, null cestu zruší. Manažer nástrojů ji dostane hned, aby šel nástroj zkontrolovat;
     * v režimu hned se zapíše i do nastavení.
     */
    public void setPath(ExternalUtil util, File path) {
        current.put(util, path);
        target.setPath(util, path);
        if (immediate) {
            config.setFile(ExternalTools.configKey(util), path);
            saved.put(util, path);
        }
    }

    /** Nástroje, jejichž platná cesta se liší od uložené (v režimu hned vždy prázdné). */
    public Set<ExternalUtil> getChanged() {
        Set<ExternalUtil> changed = EnumSet.noneOf(ExternalUtil.class);
        for (ExternalUtil util : ExternalUtil.values()) {
            if (!Objects.equals(saved.get(util), current.get(util))) {
                changed.add(util);
            }
        }
        return Collections.unmodifiableSet(changed);
    }

    public boolean isChanged() {
        return !getChanged().isEmpty();
    }

    /** Zapíše změněné cesty do nastavení (nezměněné se nezapisují). */
    public void save() {
        for (ExternalUtil util : getChanged()) {
            config.setFile(ExternalTools.configKey(util), current.get(util));
            saved.put(util, current.get(util));
        }
    }

    /**
     * Zahodí neuložené cesty: manažer nástrojů dostane zpět uložené cesty.
     *
     * @return nástroje, jejichž cesta se vrátila (je potřeba je znovu zkontrolovat, aby dostupnost odpovídala
     * uloženému nastavení)
     */
    public Set<ExternalUtil> discard() {
        Set<ExternalUtil> reverted = getChanged();
        for (ExternalUtil util : reverted) {
            current.put(util, saved.get(util));
            target.setPath(util, saved.get(util));
        }
        return reverted;
    }
}
