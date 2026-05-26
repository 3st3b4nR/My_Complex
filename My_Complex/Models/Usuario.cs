using System.ComponentModel.DataAnnotations;

namespace My_Complex.Models
{
    public class Usuario
    {
        [Key]
        public int id_usuario { get; set; }

        public string Nombre { get; set; } = string.Empty;
        public string Apellido { get; set; } = string.Empty;
        public string Documento { get; set; } = string.Empty;
        public string Correo { get; set; } = string.Empty;
        public string Clave { get; set; } = string.Empty;

        public int? Apartamento_id_apartamento { get; set; }

        // Relaciones
        public ICollection<UsuarioRol> UsuarioRoles { get; set; } = new List<UsuarioRol>();
        public ICollection<Apartamento> Apartamentos { get; set; } = new List<Apartamento>();
        public ICollection<Telefono> Telefonos { get; set; } = new List<Telefono>();
    }
}
