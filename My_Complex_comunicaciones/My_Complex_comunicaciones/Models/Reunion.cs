using System.ComponentModel.DataAnnotations;

namespace My_Comunicacion.Models
{
    public class Reunion
    {
        [Key]
        public int id_reunion { get; set; }
        public string Titulo { get; set; } = string.Empty;
        public string Descripcion { get; set; } = string.Empty;
        public DateTime Fecha { get; set; }
        public string Lugar { get; set; } = string.Empty;
        public DateTime Hora { get; set; }
        public int Usuario_id { get; set; }
    }
}
