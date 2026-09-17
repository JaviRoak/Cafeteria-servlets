package cafeteria.modelo;

import java.util.List;

public class Pedido {
    private final int idPedido;
    private final int idCliente;
    private final List<LineaPedido> lineas;
    private final double total;
    private String estado;

    public Pedido(int idPedido, int idCliente, List<LineaPedido> lineas, double total) {
        this.idPedido = idPedido;
        this.idCliente = idCliente;
        this.lineas = lineas;
        this.total = total;
        this.estado = "Pendiente";
    }

    public int getIdPedido() { return idPedido; }
    public int getIdCliente() { return idCliente; }
    public List<LineaPedido> getLineas() { return lineas; }
    public double getTotal() { return total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
