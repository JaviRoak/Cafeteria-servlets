package cafeteria.modelo;

public class LineaPedido {
    private final int idProducto;
    private final int cantidad;

    public LineaPedido(int idProducto, int cantidad) {
        this.idProducto = idProducto;
        this.cantidad = cantidad;
    }

    public int getIdProducto() { return idProducto; }
    public int getCantidad() { return cantidad; }
}
