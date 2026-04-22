using My_Complex.Models;

namespace My_Complex.Services
{
    public interface IJwtService
    {
        string GenerateToken(User user, out DateTime expiresAt);
    }
}
