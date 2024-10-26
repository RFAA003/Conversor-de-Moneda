import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import java.net.URL;
import java.util.*;
import java.text.NumberFormat;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class CurrencyConverter {
    private final Scanner scanner;
    private final Map<String, Double> cachedRates;
    private String lastUpdateDate;
    private final List<String> historialConversiones;

    private static final Map<String, String> CURRENCIES;

    static {
        CURRENCIES = new HashMap<String, String>();
        CURRENCIES.put("ARS", "Peso argentino");
        CURRENCIES.put("BOB", "Boliviano");
        CURRENCIES.put("BRL", "Real brasileño");
        CURRENCIES.put("CLP", "Peso chileno");
        CURRENCIES.put("COP", "Peso colombiano");
        CURRENCIES.put("USD", "Dólar estadounidense");
    }

    public CurrencyConverter() {
        this.scanner = new Scanner(System.in);
        this.cachedRates = new HashMap<String, Double>();
        this.historialConversiones = new ArrayList<String>();
    }

    public void iniciarPrograma() {
        mostrarBienvenida();

        boolean continuar = true;
        while (continuar) {
            try {
                mostrarMenuPrincipal();
                int opcion = leerOpcion(0, 4);

                switch (opcion) {
                    case 1:
                        realizarConversion();
                        break;
                    case 2:
                        mostrarMonedasDisponibles();
                        break;
                    case 3:
                        mostrarTasasCambio();
                        break;
                    case 4:
                        mostrarHistorialConversiones();
                        break;
                    case 0:
                        continuar = confirmarSalida();
                        break;
                }
            } catch (Exception e) {
                manejarError(e);
            }
        }
        mostrarDespedida();
    }

    private JsonObject realizarPeticionAPI() throws Exception {
        final String API_KEY = "5411583e1975fbd5e852cc8c";
        URL url = new URL("https://api.exchangerate-api.com/v4/latest/USD");
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();

        try {
            reader = new BufferedReader(
                    new InputStreamReader(url.openStream())
            );

            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // Ignorar error al cerrar
                }
            }
        }

        return JsonParser.parseString(response.toString()).getAsJsonObject();
    }

    private void actualizarTasasCambio() throws Exception {
        JsonObject json = realizarPeticionAPI();
        JsonObject rates = json.getAsJsonObject("rates");

        cachedRates.clear();
        Iterator<String> it = CURRENCIES.keySet().iterator();
        while(it.hasNext()) {
            String moneda = it.next();
            if (rates.has(moneda)) {
                cachedRates.put(moneda, rates.get(moneda).getAsDouble());
            }
        }

        lastUpdateDate = json.get("date").getAsString();
    }

    private void mostrarBienvenida() {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║    CONVERTIDOR DE MONEDAS v1.0     ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.println("\nBienvenido al sistema de conversión de monedas");

        try {
            actualizarTasasCambio();
        } catch (Exception e) {
            System.out.println("Error al cargar tasas de cambio: " + e.getMessage());
        }
    }

    private void mostrarMenuPrincipal() {
        System.out.println("\n=== MENÚ PRINCIPAL ===");
        System.out.println("1. Realizar conversión");
        System.out.println("2. Ver monedas disponibles");
        System.out.println("3. Consultar tasas de cambio");
        System.out.println("4. Ver historial");
        System.out.println("0. Salir");
        System.out.print("Seleccione una opción: ");
    }

    private void realizarConversion() throws Exception {
        mostrarMonedasDisponibles();

        String monedaOrigen = solicitarMoneda("origen");
        String monedaDestino = solicitarMoneda("destino");
        double cantidad = solicitarCantidad();

        if (cachedRates.isEmpty()) {
            actualizarTasasCambio();
        }

        double resultado = convertir(monedaOrigen, monedaDestino, cantidad);
        mostrarResultadoConversion(monedaOrigen, monedaDestino, cantidad, resultado);
        guardarEnHistorial(monedaOrigen, monedaDestino, cantidad, resultado);
    }

    private String solicitarMoneda(String tipo) {
        while (true) {
            System.out.print("\nIngrese el código de la moneda de " + tipo + ": ");
            String moneda = scanner.nextLine().toUpperCase().trim();

            if (CURRENCIES.containsKey(moneda)) {
                return moneda;
            }
            System.out.println("Moneda no válida. Por favor, elija de la lista:");
            mostrarMonedasDisponibles();
        }
    }

    private double solicitarCantidad() {
        while (true) {
            System.out.print("Ingrese la cantidad a convertir: ");
            try {
                String input = scanner.nextLine().replace(",", ".");
                double cantidad = Double.parseDouble(input);
                if (cantidad > 0) {
                    return cantidad;
                }
                System.out.println("Por favor ingrese un valor mayor a 0");
            } catch (NumberFormatException e) {
                System.out.println("Por favor ingrese un número válido");
            }
        }
    }

    private void mostrarMonedasDisponibles() {
        System.out.println("\nMonedas Disponibles:");
        Iterator<Map.Entry<String, String>> it = CURRENCIES.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }

    private double convertir(String monedaOrigen, String monedaDestino, double cantidad) {
        double tasaOrigen = cachedRates.get(monedaOrigen).doubleValue();
        double tasaDestino = cachedRates.get(monedaDestino).doubleValue();
        return cantidad * (tasaDestino / tasaOrigen);
    }

    private void mostrarResultadoConversion(String monedaOrigen, String monedaDestino,
                                            double cantidad, double resultado) {
        System.out.println("\nResultado de la conversión:");
        System.out.printf("%.2f %s = %.2f %s%n",
                cantidad, monedaOrigen, resultado, monedaDestino);
        System.out.println("Fecha de tasa: " + lastUpdateDate);
    }

    private void mostrarTasasCambio() throws Exception {
        if (cachedRates.isEmpty()) {
            actualizarTasasCambio();
        }

        System.out.println("\nTasas de cambio actuales (USD base):");
        Iterator<Map.Entry<String, Double>> it = cachedRates.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, Double> entry = it.next();
            System.out.printf("%s: %.4f%n", entry.getKey(), entry.getValue());
        }
    }

    private void guardarEnHistorial(String monedaOrigen, String monedaDestino,
                                    double cantidad, double resultado) {
        String conversion = String.format("%s %.2f %s = %.2f %s",
                lastUpdateDate, cantidad, monedaOrigen, resultado, monedaDestino);
        historialConversiones.add(conversion);
    }

    private void mostrarHistorialConversiones() {
        if (historialConversiones.isEmpty()) {
            System.out.println("\nNo hay conversiones en el historial.");
            return;
        }

        System.out.println("\nHistorial de conversiones:");
        for (int i = 0; i < historialConversiones.size(); i++) {
            System.out.println((i + 1) + ". " + historialConversiones.get(i));
        }
    }

    private int leerOpcion(int min, int max) {
        while (true) {
            try {
                int opcion = Integer.parseInt(scanner.nextLine().trim());
                if (opcion >= min && opcion <= max) {
                    return opcion;
                }
                System.out.print("Ingrese un número entre " + min + " y " + max + ": ");
            } catch (NumberFormatException e) {
                System.out.print("Por favor ingrese un número válido: ");
            }
        }
    }

    private boolean confirmarSalida() {
        System.out.print("¿Está seguro que desea salir? (s/n): ");
        String respuesta = scanner.nextLine().trim().toLowerCase();
        return respuesta.equals("s") || respuesta.equals("si");
    }

    private void mostrarDespedida() {
        System.out.println("\n¡Gracias por usar el convertidor!");
    }

    private void manejarError(Exception e) {
        System.out.println("Error: " + e.getMessage());
        System.out.println("Presione Enter para continuar...");
        scanner.nextLine();
    }

    public static void main(String[] args) {
        CurrencyConverter converter = new CurrencyConverter();
        converter.iniciarPrograma();
    }
}