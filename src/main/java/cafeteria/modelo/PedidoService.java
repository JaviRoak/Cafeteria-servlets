package cafeteria.modelo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modelo de prueba: guarda los datos en memoria (sin base de datos).
 * Los datos se pierden al reiniciar el servidor.
 */
public class PedidoService {

    private static final Map<Integer, Producto> productos = new ConcurrentHashMap<>();
    private static final Map<Integer, Pedido> pedidos = new ConcurrentHashMap<>();
    private static final AtomicInteger secuencia = new AtomicInteger(0);

    static {
        productos.put(3, new Producto(3, "Café americano", 1.50, 10));
        productos.put(7, new Producto(7, "Croissant", 2.25, 1));
    }

    public Pedido crearPedido(int idCliente, List<LineaPedido> lineas)
            throws ProductoNoEncontradoException {
        double total = 0;
        for (LineaPedido l : lineas) {
            Producto p = productos.get(l.getIdProducto());
            if (p == null) {
                throw new ProductoNoEncontradoException("Producto " + l.getIdProducto());
            }
            total += p.getPrecio() * l.getCantidad();
        }
        Pedido pedido = new Pedido(secuencia.incrementAndGet(), idCliente, lineas, total);
        pedidos.put(pedido.getIdPedido(), pedido);
        return pedido;
    }

    // synchronized simula la transacción: dos confirmaciones no se cruzan
    public synchronized Pedido confirmarPedido(int idPedido)
            throws PedidoNoEncontradoException, EstadoInvalidoException,
                   StockInsuficienteException {
        Pedido pedido = pedidos.get(idPedido);
        if (pedido == null) throw new PedidoNoEncontradoException("Pedido " + idPedido);
        if (!pedido.getEstado().equals("Pendiente")) {
            throw new EstadoInvalidoException("Estado actual: " + pedido.getEstado());
        }
        // 1. Verificar stock de todas las líneas antes de descontar
        for (LineaPedido l : pedido.getLineas()) {
            if (productos.get(l.getIdProducto()).getStock() < l.getCantidad()) {
                throw new StockInsuficienteException("Producto " + l.getIdProducto());
            }
        }
        // 2. Descontar stock y confirmar
        for (LineaPedido l : pedido.getLineas()) {
            Producto p = productos.get(l.getIdProducto());
            p.setStock(p.getStock() - l.getCantidad());
        }
        pedido.setEstado("Confirmado");
        return pedido;
    }

    public Pedido obtenerPedido(int idPedido) {
        return pedidos.get(idPedido);
    }
}
