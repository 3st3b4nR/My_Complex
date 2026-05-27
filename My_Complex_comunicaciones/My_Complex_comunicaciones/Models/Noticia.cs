using System.ComponentModel.DataAnnotations;

namespace My_Comunicacion.Models
{
    public class Noticia
    {
        [Key]
        public int id_noticia { get; set; }
        public string Titulo { get; set; } = string.Empty;
        public string Contenido { get; set; } = string.Empty;
        public DateTime Fecha { get; set; }
        public int Usuario_id { get; set; }
    }
}