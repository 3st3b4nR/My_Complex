namespace My_Complex.DTOs
{
    public class UpdateUserRequest
    {
        public int UsuarioId { get; set; }          // ID del usuario a actualizar
        public string? NuevoTelefono { get; set; }  // Teléfono opcional
        public string? NuevaClave { get; set; }     // Contraseña opcional
    }
}
