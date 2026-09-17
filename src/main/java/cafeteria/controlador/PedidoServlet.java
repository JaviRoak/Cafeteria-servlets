package cafeteria.controlador;

import cafeteria.modelo.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de pedidos (MVC).
 *   POST /pedidos                  -> crear pedido (estado Pendiente)
 *   GET  /pedidos/{id}             -> consultar pedido y su estado
 *   POST /pedidos/{id}/confirmar   -> validar stock y confirmar pedido
 * Responde siempre en JSON.
 */
@WebServlet("/pedidos/*")
public class PedidoServlet extends HttpServlet {

    private PedidoService pedidoService;   // Modelo

    // Ciclo de vida: se ejecuta una sola vez, al cargar el servlet
    @Override
    public void init() throws ServletException {
        pedidoService = new PedidoService();
    }

    // ---------- GET: solo consulta, no modifica el estado ----------
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String[] partes = segmentosRuta(req);

        if (partes.length == 1) {
            consultarPedido(partes[0], res);
        } else {
            responder(res, 404, error("Ruta no encontrada"));
        }
    }

    // ---------- POST: acciones que modifican datos ----------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        req.setCharacterEncoding("UTF-8");  // antes de leer parámetros
        String[] partes = segmentosRuta(req);

        if (partes.length == 0) {
            crearPedido(req, res);
        } else if (partes.length == 2 && partes[1].equals("confirmar")) {
            confirmarPedido(partes[0], res);
        } else {
            responder(res, 404, error("Ruta no encontrada"));
        }
    }

    // ===== Acción 1: crear pedido =====
    private void crearPedido(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String idClienteTxt = req.getParameter("idCliente");
        String[] idsProducto = req.getParameterValues("idProducto");
        String[] cantidades  = req.getParameterValues("cantidad");

        // 1. Campos obligatorios (regla: al menos un producto)
        if (idClienteTxt == null || idsProducto == null || cantidades == null) {
            responder(res, 400, error("Faltan campos: idCliente, idProducto, cantidad"));
            return;
        }
        if (idsProducto.length != cantidades.length) {
            responder(res, 400, error("Cada producto debe tener su cantidad"));
            return;
        }

        // 2. Formato y rangos
        int idCliente;
        List<LineaPedido> lineas = new ArrayList<>();
        try {
            idCliente = Integer.parseInt(idClienteTxt.trim());
            for (int i = 0; i < idsProducto.length; i++) {
                int idProducto = Integer.parseInt(idsProducto[i].trim());
                int cantidad   = Integer.parseInt(cantidades[i].trim());
                if (cantidad <= 0) {
                    responder(res, 400, error("La cantidad debe ser mayor que cero"));
                    return;
                }
                lineas.add(new LineaPedido(idProducto, cantidad));
            }
        } catch (NumberFormatException e) {
            responder(res, 400, error("Los identificadores y cantidades deben ser enteros"));
            return;
        }

        // 3. Modelo: crea el pedido en estado Pendiente y calcula el total
        try {
            Pedido pedido = pedidoService.crearPedido(idCliente, lineas);
            responder(res, 201, pedidoJson(pedido));
        } catch (ProductoNoEncontradoException e) {
            responder(res, 404, error("Uno de los productos no existe"));
        } catch (Exception e) {
            log("Error al crear pedido", e);  // detalle solo en el servidor
            responder(res, 500, error("No se pudo crear el pedido"));
        }
    }

    // ===== Acción 2: confirmar pedido =====
    private void confirmarPedido(String idTxt, HttpServletResponse res)
            throws IOException {
        Integer idPedido = aEntero(idTxt);
        if (idPedido == null) {
            responder(res, 400, error("El id del pedido debe ser un entero"));
            return;
        }

        // Modelo: valida stock, lo descuenta y cambia a Confirmado
        try {
            Pedido pedido = pedidoService.confirmarPedido(idPedido);
            responder(res, 200, pedidoJson(pedido));
        } catch (PedidoNoEncontradoException e) {
            responder(res, 404, error("El pedido no existe"));
        } catch (EstadoInvalidoException e) {
            responder(res, 409, error("Solo se confirman pedidos en estado Pendiente"));
        } catch (StockInsuficienteException e) {
            responder(res, 409, error("Stock insuficiente para confirmar el pedido"));
        } catch (Exception e) {
            log("Error al confirmar pedido " + idPedido, e);
            responder(res, 500, error("No se pudo confirmar el pedido"));
        }
    }

    // ===== Acción 3: consultar pedido =====
    private void consultarPedido(String idTxt, HttpServletResponse res)
            throws IOException {
        Integer idPedido = aEntero(idTxt);
        if (idPedido == null) {
            responder(res, 400, error("El id del pedido debe ser un entero"));
            return;
        }
        Pedido pedido = pedidoService.obtenerPedido(idPedido);
        if (pedido == null) {
            responder(res, 404, error("El pedido no existe"));
        } else {
            responder(res, 200, pedidoJson(pedido));
        }
    }

    // ---------- Utilidades ----------

    // "/125/confirmar" -> ["125", "confirmar"];  null o "/" -> []
    private String[] segmentosRuta(HttpServletRequest req) {
        String ruta = req.getPathInfo();
        if (ruta == null || ruta.equals("/")) return new String[0];
        return ruta.substring(1).split("/");
    }

    private Integer aEntero(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void responder(HttpServletResponse res, int estado, String json)
            throws IOException {
        res.setStatus(estado);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        try (PrintWriter out = res.getWriter()) {   // libera el writer
            out.print(json);
        }
    }

    // Solo se devuelven datos del pedido, nunca datos personales del cliente
    private String pedidoJson(Pedido p) {
        return String.format(Locale.US,
            "{\"idPedido\": %d, \"estado\": \"%s\", \"total\": %.2f}",
            p.getIdPedido(), p.getEstado(), p.getTotal());
    }

    private String error(String mensaje) {
        return "{\"error\": \"" + mensaje + "\"}";
    }

    // Ciclo de vida: se ejecuta al descargar el servlet
    @Override
    public void destroy() {
        pedidoService = null;
    }
}
