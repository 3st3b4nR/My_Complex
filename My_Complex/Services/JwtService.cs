using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.IdentityModel.Tokens;
using System.Text;
using Microsoft.Extensions.Options;
using My_Complex.Config;
using My_Complex.Models;


namespace My_Complex.Services
{
    public class JwtService : IJwtService
    {
        private readonly JwtSettings _jwt;

        public JwtService(IOptions<JwtSettings> jwt)
        {
            _jwt = jwt.Value;
        }

        public string GenerateToken(User user, out DateTime expiresAt)
        {
            var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_jwt.Key));
            var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);


            var claims = new[]
            {
                new Claim(JwtRegisteredClaimNames.Sub, user.Id),
                new Claim(JwtRegisteredClaimNames.UniqueName, user.Email),
                new Claim("role", user.Role)
            };

            expiresAt = DateTime.UtcNow.AddMinutes(_jwt.ExpiresInMinutes);

            var token = new JwtSecurityToken(
                issuer: _jwt.Issuer,
                audience: _jwt.Audience,
                claims: claims,
                expires: expiresAt,
                signingCredentials: creds
            );

            return new JwtSecurityTokenHandler().WriteToken(token);
        }
    }
}
