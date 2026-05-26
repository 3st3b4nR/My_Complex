using System.ComponentModel.DataAnnotations;

namespace My_Complex.Models
{
    public class Rol
    {
        [Key]
        public int Id_rol { get; set; }

        public string Nombre_rol { get; set; } = string.Empty;

        public ICollection<UsuarioRol> UsuarioRoles { get; set; } = new List<UsuarioRol>();
    }
}
