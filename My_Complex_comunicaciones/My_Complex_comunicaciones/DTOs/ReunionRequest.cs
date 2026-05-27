namespace My_Comunicacion.DTOs
{
    public class ReunionRequest
    {
        public string Titulo { get; set; }
        public string Descripcion { get; set; }
        public DateTime Fecha { get; set; }
        public string Lugar { get; set; }
        public DateTime Hora { get; set; }
        public int Usuario_id { get; set; }
    }
}
