package com.example.simplebrowser;

import android.content.Context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Localization {

    public static final String SYSTEM = "system";
    public static final String ENGLISH = "en";
    public static final String SPANISH = "es";
    public static final String PORTUGUESE = "pt";
    public static final String FRENCH = "fr";

    private static final Map<String, String[]> TRANSLATIONS =
            new HashMap<>();

    private static final Map<String, String> FRENCH_TRANSLATIONS =
            new HashMap<>();


    static {
        add("Settings", "Configuración", "Configurações");
        add("Privacy & Security", "Privacidad y seguridad", "Privacidade e segurança");
        add("Desktop mode", "Modo de escritorio", "Modo desktop");
        add("Enter Incognito Mode", "Entrar en modo incógnito", "Entrar no modo anônimo");
        add("Exit Incognito Mode", "Salir del modo incógnito", "Sair do modo anônimo");
        add("Image", "Imagen", "Imagem");
        add("Audio", "Audio", "Áudio");
        add("Video", "Vídeo", "Vídeo");
        add("Link", "Enlace", "Link");
        add("Open", "Abrir", "Abrir");
        add("Open in new tab", "Abrir en una pestaña nueva", "Abrir em uma nova aba");
        add("Download", "Descargar", "Baixar");
        add("Share", "Compartir", "Compartilhar");
        add("Copy URL", "Copiar URL", "Copiar URL");
        add("Copy link", "Copiar enlace", "Copiar link");
        add("Image URL", "URL de imagen", "URL da imagem");
        add("Audio URL", "URL de audio", "URL do áudio");
        add("Video URL", "URL de vídeo", "URL do vídeo");
        add("Copied", "Copiado", "Copiado");
        add("No app can share this", "Ninguna aplicación puede compartir esto", "Nenhum aplicativo pode compartilhar isto");
        add("General", "General", "Geral");
        add("Privacy &amp; Security", "Privacidad y seguridad", "Privacidade e segurança");
        add("Websites", "Sitios web", "Sites");
        add("Appearance", "Apariencia", "Aparência");
        add("Advanced", "Avanzado", "Avançado");
        add("Cookies", "Cookies", "Cookies");

        add("Configure Simple Browser", "Configura Simple Browser", "Configure o Simple Browser");
        add("Home page", "Página de inicio", "Página inicial");
        add("Search engine", "Motor de búsqueda", "Mecanismo de pesquisa");
        add("Custom home page", "Página de inicio personalizada", "Página inicial personalizada");
        add("Custom search URL", "URL de búsqueda personalizada", "URL de pesquisa personalizada");
        add("Restore tabs on startup", "Restaurar pestañas al iniciar", "Restaurar abas ao iniciar");
        add("Reopen your normal tabs when Simple Browser starts again.", "Vuelve a abrir tus pestañas normales cuando Simple Browser se inicie de nuevo.", "Reabra suas abas normais quando o Simple Browser iniciar novamente.");
        add("Use a complete URL such as https://example.com/", "Usa una URL completa, como https://example.com/", "Use uma URL completa, como https://example.com/");
        add("Use %s where the search text should be inserted.", "Usa %s donde se debe insertar el texto de búsqueda.", "Use %s onde o texto da pesquisa deve ser inserido.");
        add("Clear browsing data", "Borrar datos de navegación", "Limpar dados de navegação");
        add("Restore default settings", "Restaurar configuración predeterminada", "Restaurar configurações padrão");

        add("JavaScript", "JavaScript", "JavaScript");
        add("Allow websites to run JavaScript", "Permitir que los sitios web ejecuten JavaScript", "Permitir que os sites executem JavaScript");
        add("Pop-ups", "Ventanas emergentes", "Pop-ups");
        add("Allow websites to open new windows", "Permitir que los sitios web abran nuevas ventanas", "Permitir que os sites abram novas janelas");
        add("Allow websites to store cookies", "Permitir que los sitios web almacenen cookies", "Permitir que os sites armazenem cookies");
        add("Website storage", "Almacenamiento de sitios web", "Armazenamento de sites");
        add("Allow websites to use local storage", "Permitir que los sitios web usen almacenamiento local", "Permitir que os sites usem armazenamento local");
        add("Images", "Imágenes", "Imagens");
        add("Allow websites to load network images", "Permitir que los sitios web carguen imágenes de red", "Permitir que os sites carreguem imagens da rede");
        add("Zoom", "Zoom", "Zoom");
        add("Allow page zoom and pinch-to-zoom", "Permitir zoom de página y pellizcar para ampliar", "Permitir zoom da página e pinça para ampliar");
        add("Location access", "Acceso a la ubicación", "Acesso à localização");
        add("Allow websites to request device location", "Permitir que los sitios web soliciten la ubicación del dispositivo", "Permitir que os sites solicitem a localização do dispositivo");
        add("Media autoplay", "Reproducción automática de medios", "Reprodução automática de mídia");
        add("Allow audio and video to start without a user gesture", "Permitir que el audio y el vídeo se inicien sin una acción del usuario", "Permitir que áudio e vídeo iniciem sem uma ação do usuário");

        add("Browser information", "Información del navegador", "Informações do navegador");
        add("Developer logs", "Registros de desarrollador", "Registros do desenvolvedor");
        add("Network requests, navigation changes, load errors, and JavaScript console messages. Logs are in memory only and capped automatically.", "Solicitudes de red, cambios de navegación, errores de carga y mensajes de la consola de JavaScript. Los registros solo se guardan en memoria y tienen un límite automático.", "Solicitações de rede, alterações de navegação, erros de carregamento e mensagens do console JavaScript. Os registros ficam apenas na memória e têm um limite automático.");
        add("Refresh logs", "Actualizar registros", "Atualizar registros");
        add("Clear logs", "Borrar registros", "Limpar registros");
        add("User agent", "Agente de usuario", "Agente do usuário");
        add("Choose how websites identify this browser. Desktop mode still takes priority while it is enabled.", "Elige cómo identifican los sitios web a este navegador. El modo de escritorio sigue teniendo prioridad mientras esté activado.", "Escolha como os sites identificam este navegador. O modo desktop continua tendo prioridade enquanto estiver ativado.");
        add("Custom user agent", "Agente de usuario personalizado", "Agente do usuário personalizado");
        add("Enter a complete User-Agent string.", "Introduce una cadena User-Agent completa.", "Digite uma cadeia User-Agent completa.");
        add("WebView debugging", "Depuración de WebView", "Depuração do WebView");
        add("Allow Chrome-based developer tools to inspect Simple Browser WebViews.", "Permite que las herramientas de desarrollador basadas en Chrome inspeccionen los WebView de Simple Browser.", "Permite que as ferramentas de desenvolvedor baseadas no Chrome inspecionem os WebViews do Simple Browser.");
        add("WebView cache", "Caché de WebView", "Cache do WebView");
        add("Clear cached website resources from every open tab.", "Borra los recursos web almacenados en caché de todas las pestañas abiertas.", "Limpa os recursos de sites em cache de todas as abas abertas.");
        add("Clear WebView cache", "Borrar caché de WebView", "Limpar cache do WebView");
        add("Current WebView", "WebView actual", "WebView atual");
        add("Inspect the active tab URL and user agent.", "Consulta la URL y el agente de usuario de la pestaña activa.", "Veja a URL e o agente do usuário da aba ativa.");
        add("Show information", "Mostrar información", "Mostrar informações");
        add("Browser color", "Color del navegador", "Cor do navegador");
        add("Changes the toolbar, tabs, menu, and every part of the built-in settings UI.", "Cambia la barra de herramientas, las pestañas, el menú y todas las partes de la interfaz de configuración integrada.", "Altera a barra de ferramentas, abas, menu e todas as partes da interface de configurações integrada.");

        add("Language", "Idioma", "Idioma");
        add("Choose the browser language. System default follows the closest supported language.", "Elige el idioma del navegador. El valor predeterminado del sistema usa el idioma compatible más cercano.", "Escolha o idioma do navegador. O padrão do sistema usa o idioma compatível mais próximo.");
        add("System default", "Predeterminado del sistema", "Padrão do sistema");
        add("English", "English", "English");
        add("Spanish", "Español", "Español");
        add("Portuguese", "Português", "Português");
        add("French", "Francés", "Français");
        add("Blank page", "Página en blanco", "Página em branco");
        add("Default page", "Página predeterminada", "Página padrão");
        add("Custom", "Personalizado", "Personalizado");
        add("Cookie could not be changed", "No se pudo cambiar la cookie", "Não foi possível alterar o cookie");
        add("Cookie could not be deleted", "No se pudo eliminar la cookie", "Não foi possível excluir o cookie");
        add("Some site cookies could not be deleted", "No se pudieron eliminar algunas cookies del sitio", "Alguns cookies do site não puderam ser excluídos");

        add("New Tab", "Nueva pestaña", "Nova aba");
        add("Search or enter an address", "Busca o introduce una dirección", "Pesquise ou digite um endereço");

        add("History", "Historial", "Histórico");
        add("Browsing history is not saved in Incognito Mode.", "El historial de navegación no se guarda en el modo incógnito.", "O histórico de navegação não é salvo no modo anônimo.");
        add("Pages you've visited in Simple Browser.", "Páginas que has visitado en Simple Browser.", "Páginas que você visitou no Simple Browser.");
        add("Search history", "Buscar en el historial", "Pesquisar no histórico");
        add("Search", "Buscar", "Pesquisar");
        add("Clear", "Borrar", "Limpar");
        add("Nothing is shown here while browsing privately.", "No se muestra nada aquí mientras navegas de forma privada.", "Nada é mostrado aqui durante a navegação privada.");
        add("No history entries found.", "No se encontraron entradas del historial.", "Nenhuma entrada do histórico encontrada.");
        add("Loading more...", "Cargando más...", "Carregando mais...");
        add("Delete", "Eliminar", "Excluir");

        add("Downloads", "Descargas", "Downloads");
        add("Download history is not saved in Incognito Mode.", "El historial de descargas no se guarda en el modo incógnito.", "O histórico de downloads não é salvo no modo anônimo.");
        add("Files you've downloaded from Simple Browser.", "Archivos que has descargado desde Simple Browser.", "Arquivos que você baixou do Simple Browser.");
        add("No downloads found.", "No se encontraron descargas.", "Nenhum download encontrado.");
        add("Download", "Descargar", "Baixar");
        add("Status unavailable", "Estado no disponible", "Status indisponível");
        add("Download record unavailable", "Registro de descarga no disponible", "Registro de download indisponível");
        add("Completed", "Completado", "Concluído");
        add("Failed", "Fallido", "Falhou");
        add("Downloading", "Descargando", "Baixando");
        add("Downloads history cleared", "Historial de descargas borrado", "Histórico de downloads limpo");
        add("Download is not ready", "La descarga no está lista", "O download não está pronto");
        add("No app can open this download", "Ninguna aplicación puede abrir esta descarga", "Nenhum aplicativo pode abrir este download");

        add("Cookie data is temporary in Incognito Mode.", "Los datos de cookies son temporales en el modo incógnito.", "Os dados de cookies são temporários no modo anônimo.");
        add("Cookies are not managed from Incognito Mode.", "Las cookies no se administran desde el modo incógnito.", "Os cookies não são gerenciados no modo anônimo.");
        add("Websites discovered with cookies in Simple Browser, based on visited sites and open tabs.", "Sitios web con cookies descubiertos en Simple Browser, según los sitios visitados y las pestañas abiertas.", "Sites descobertos com cookies no Simple Browser, com base nos sites visitados e nas abas abertas.");
        add("No cookies are currently known.", "No se conocen cookies actualmente.", "Nenhum cookie conhecido no momento.");
        add("Edit", "Editar", "Editar");
        add("Delete site cookies", "Eliminar cookies del sitio", "Excluir cookies do site");
        add("Site cookies deleted", "Cookies del sitio eliminadas", "Cookies do site excluídos");
        add("Edit cookie: ", "Editar cookie: ", "Editar cookie: ");
        add("Save", "Guardar", "Salvar");
        add("Cancel", "Cancelar", "Cancelar");

        add("Page unavailable", "Página no disponible", "Página indisponível");
        add("The page could not be loaded.", "No se pudo cargar la página.", "Não foi possível carregar a página.");
        add("This page isn't working", "Esta página no funciona", "Esta página não está funcionando");
        add("Go back", "Volver", "Voltar");

        add("WebView information", "Información de WebView", "Informações do WebView");
        add("URL:\n", "URL:\n", "URL:\n");
        add("User agent:\n", "Agente de usuario:\n", "Agente do usuário:\n");
        add("Android API: ", "API de Android: ", "API do Android: ");
        add("OK", "Aceptar", "OK");

        add("Download started: ", "Descarga iniciada: ", "Download iniciado: ");
        add("Unable to start download", "No se pudo iniciar la descarga", "Não foi possível iniciar o download");
        add("Browsing data cleared", "Datos de navegación borrados", "Dados de navegação limpos");
        add("WebView cache cleared", "Caché de WebView borrada", "Cache do WebView limpo");
        add("Stop loading", "Detener carga", "Parar carregamento");
        add("Reload", "Recargar", "Recarregar");
        add("Home", "Inicio", "Início");
        add("Back", "Atrás", "Voltar");
        add("Forward", "Adelante", "Avançar");
        add("Settings", "Configuración", "Configurações");
        french("Settings", "Paramètres");
french("Cookies", "Cookies");
        french("Cookie data is temporary in Incognito Mode.", "Les données des cookies sont temporaires en mode navigation privée.");
        french("Cookies are not managed from Incognito Mode.", "Les cookies ne sont pas gérés en mode navigation privée.");
        french("Websites discovered with cookies in Simple Browser, based on visited sites and open tabs.", "Sites ayant des cookies détectés dans Simple Browser, selon les sites visités et les onglets ouverts.");
        french("No cookies are currently known.", "Aucun cookie connu pour le moment.");
        french("Edit", "Modifier");
        french("Delete", "Supprimer");
        french("Delete site cookies", "Supprimer les cookies du site");
        french("Site cookies deleted", "Cookies du site supprimés");
        french("Edit cookie: ", "Modifier le cookie : ");
        french("Save", "Enregistrer");
        french("Cancel", "Annuler");
        french("General", "Général");
        french("Privacy &amp; Security", "Confidentialité et sécurité");
        french("Websites", "Sites web");
        french("Appearance", "Apparence");
        french("Advanced", "Avancé");
        french("Cookies", "Cookies");
        french("Configure Simple Browser", "Configurer Simple Browser");
        french("Home page", "Page d’accueil");
        french("Search engine", "Moteur de recherche");
        french("Custom home page", "Page d’accueil personnalisée");
        french("Custom search URL", "URL de recherche personnalisée");
        french("Restore tabs on startup", "Restaurer les onglets au démarrage");
        french("Reopen your normal tabs when Simple Browser starts again.", "Rouvrir vos onglets normaux au prochain démarrage de Simple Browser.");
        french("Use a complete URL such as https://example.com/", "Utilisez une URL complète telle que https://example.com/");
        french("Use %s where the search text should be inserted.", "Utilisez %s à l’emplacement du texte de recherche.");
        french("Clear browsing data", "Effacer les données de navigation");
        french("Restore default settings", "Restaurer les paramètres par défaut");
        french("JavaScript", "JavaScript");
        french("Allow websites to run JavaScript", "Autoriser les sites web à exécuter JavaScript");
        french("Pop-ups", "Fenêtres pop-up");
        french("Allow websites to open new windows", "Autoriser les sites web à ouvrir de nouvelles fenêtres");
        french("Allow websites to store cookies", "Autoriser les sites web à stocker des cookies");
        french("Website storage", "Stockage des sites web");
        french("Allow websites to use local storage", "Autoriser les sites web à utiliser le stockage local");
        french("Images", "Images");
        french("Allow websites to load network images", "Autoriser les sites web à charger des images réseau");
        french("Zoom", "Zoom");
        french("Allow page zoom and pinch-to-zoom", "Autoriser le zoom de page et le pincement pour zoomer");
        french("Location access", "Accès à la localisation");
        french("Allow websites to request device location", "Autoriser les sites web à demander la localisation de l’appareil");
        french("Media autoplay", "Lecture automatique des médias");
        french("Allow audio and video to start without a user gesture", "Autoriser l’audio et la vidéo à démarrer sans action de l’utilisateur");
        french("Browser information", "Informations sur le navigateur");
        french("Developer logs", "Journaux du développeur");
        french("Network requests, navigation changes, load errors, and JavaScript console messages. Logs are in memory only and capped automatically.", "Requêtes réseau, changements de navigation, erreurs de chargement et messages de la console JavaScript. Les journaux restent uniquement en mémoire et sont limités automatiquement.");
        french("Refresh logs", "Actualiser les journaux");
        french("Clear logs", "Effacer les journaux");
        french("User agent", "Agent utilisateur");
        french("Choose how websites identify this browser. Desktop mode still takes priority while it is enabled.", "Choisissez comment les sites web identifient ce navigateur. Le mode bureau reste prioritaire lorsqu’il est activé.");
        french("Custom user agent", "Agent utilisateur personnalisé");
        french("Enter a complete User-Agent string.", "Saisissez une chaîne User-Agent complète.");
        french("WebView debugging", "Débogage WebView");
        french("Allow Chrome-based developer tools to inspect Simple Browser WebViews.", "Autoriser les outils de développement basés sur Chrome à inspecter les WebView de Simple Browser.");
        french("WebView cache", "Cache WebView");
        french("Clear cached website resources from every open tab.", "Effacer les ressources web mises en cache de tous les onglets ouverts.");
        french("Clear WebView cache", "Effacer le cache WebView");
        french("Current WebView", "WebView actuel");
        french("Inspect the active tab URL and user agent.", "Consulter l’URL et l’agent utilisateur de l’onglet actif.");
        french("Show information", "Afficher les informations");
        french("Browser color", "Couleur du navigateur");
        french("Changes the toolbar, tabs, menu, and every part of the built-in settings UI.", "Modifie la barre d’outils, les onglets, le menu et toute l’interface intégrée des paramètres.");
        french("Language", "Langue");
        french("Choose the browser language. System default follows the closest supported language.", "Choisissez la langue du navigateur. Le réglage système utilise la langue prise en charge la plus proche.");
        french("System default", "Langue du système");
        french("English", "Anglais");
        french("Spanish", "Espagnol");
        french("Portuguese", "Portugais");
        french("Save", "Enregistrer");
        french("Cancel", "Annuler");
        french("Blank page", "Page vierge");
        french("Default page", "Page par défaut");
        french("Custom", "Personnalisé");
        french("Cookie could not be changed", "Impossible de modifier le cookie");
        french("Cookie could not be deleted", "Impossible de supprimer le cookie");
        french("Some site cookies could not be deleted", "Certains cookies du site n’ont pas pu être supprimés");
        french("OK", "OK");

    }

    private Localization() {
    }

    private static void add(
            String english,
            String spanish,
            String portuguese) {

        TRANSLATIONS.put(
                english,
                new String[] {
                        english,
                        spanish,
                        portuguese
                });
    }

    private static void french(
            String english,
            String value) {

        FRENCH_TRANSLATIONS.put(
                english,
                value);
    }

    public static String resolve(
            Context context,
            String requested) {

        String choice =
                requested == null ||
                requested.trim().isEmpty()
                        ? SYSTEM
                        : requested;

        if (!SYSTEM.equals(choice)) {
            if (SPANISH.equals(choice) ||
                    PORTUGUESE.equals(choice) ||
                    FRENCH.equals(choice) ||
                    ENGLISH.equals(choice)) {
                return choice;
            }
            return ENGLISH;
        }

        String system =
                Locale.getDefault()
                        .getLanguage();

        if (SPANISH.equalsIgnoreCase(system)) {
            return SPANISH;
        }

        if (PORTUGUESE.equalsIgnoreCase(system)) {
            return PORTUGUESE;
        }

        if (FRENCH.equalsIgnoreCase(system)) {
            return FRENCH;
        }

        return ENGLISH;
    }

    public static String translate(
            Context context,
            String value) {

        if (value == null) {
            return "";
        }

        String language =
                resolve(
                        context,
                        new BrowserSettings(context)
                                .getLanguage());

        String[] values =
                TRANSLATIONS.get(value);

        if (values == null) {
            return value;
        }

        if (SPANISH.equals(language)) {
            return values[1];
        }

        if (PORTUGUESE.equals(language)) {
            return values[2];
        }

        if (FRENCH.equals(language)) {
            String french =
                    FRENCH_TRANSLATIONS.get(value);

            return french == null
                    ? values[0]
                    : french;
        }

        return values[0];
    }

    public static String translateHtml(
            Context context,
            String html) {

        if (html == null ||
                html.isEmpty()) {
            return html;
        }

        String result = html;
        String language =
                resolve(
                        context,
                        new BrowserSettings(context)
                                .getLanguage());

        if (ENGLISH.equals(language)) {
            return result;
        }

        java.util.List<String> keys =
                new java.util.ArrayList<>(
                        TRANSLATIONS.keySet());

        java.util.Collections.sort(
                keys,
                (a, b) ->
                        Integer.compare(
                                b.length(),
                                a.length()));

        for (String key : keys) {

            String[] values =
                    TRANSLATIONS.get(key);

            String replacement;

            if (SPANISH.equals(language)) {
                replacement = values[1];
            } else if (PORTUGUESE.equals(language)) {
                replacement = values[2];
            } else if (FRENCH.equals(language)) {
                String french =
                        FRENCH_TRANSLATIONS.get(key);

                replacement =
                        french == null
                                ? values[0]
                                : french;
            } else {
                replacement = values[0];
            }

            result = result.replaceAll(
                    "(?<![A-Za-z0-9_])" +
                    Pattern.quote(key) +
                    "(?![A-Za-z0-9_])",
                    Matcher.quoteReplacement(
                            replacement));
        }

        return result;
    }
}
