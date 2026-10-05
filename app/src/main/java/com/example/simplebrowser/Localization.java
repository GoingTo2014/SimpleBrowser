package com.example.simplebrowser;

import android.content.Context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Localization {

    public static final String SYSTEM = "system";
    public static final String ENGLISH = "en";
    public static final String SPANISH = "es";
    public static final String PORTUGUESE = "pt";

    private static final Map<String, String[]> TRANSLATIONS =
            new HashMap<>();

    static {
        add("Settings", "Configuración", "Configurações");
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
        add("System default", "Predeterminado del sistema", "Padrão do sistema");
        add("English", "English", "English");
        add("Spanish", "Español", "Español");
        add("Portuguese", "Português", "Português");

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

        for (Map.Entry<String, String[]> entry :
                TRANSLATIONS.entrySet()) {

            String replacement =
                    SPANISH.equals(language)
                            ? entry.getValue()[1]
                            : entry.getValue()[2];

            result = result.replace(
                    entry.getKey(),
                    replacement);
        }

        return result;
    }
}
