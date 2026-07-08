import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { loginWithKeycloak } from "../api/authApi";

export default function Login() {
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    const formData = new FormData(e.currentTarget);
    const username = formData.get("employeeId") as string;
    const password = formData.get("password") as string;

    try {
      // Keycloak'tan token al
      const data = await loginWithKeycloak(username, password);
      
      // Token'ı localStorage'a kaydet (apiClient.ts buradan okuyacak)
      localStorage.setItem("access_token", data.access_token);
      if (data.refresh_token) {
        localStorage.setItem("refresh_token", data.refresh_token);
      }

      // Başarılı giriş sonrası Dashboard'a yönlendir
      navigate("/overview");
    } catch (err: any) {
      console.error("Giriş hatası:", err);
      setError("Kullanıcı adı veya şifre hatalı. Lütfen tekrar deneyin.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-background min-h-screen flex text-on-surface antialiased selection:bg-primary-container selection:text-on-primary-container m-0 p-0 overflow-hidden">
      {/* Left Side: Branding & Context */}
      <div className="hidden lg:flex lg:w-1/2 bg-primary-container relative flex-col justify-between p-12 overflow-hidden">
        {/* Abstract Background Elements */}
        <div 
          className="absolute inset-0 opacity-50 mix-blend-overlay pointer-events-none"
          style={{
            backgroundImage: "radial-gradient(circle at 2px 2px, rgba(255, 255, 255, 0.15) 1px, transparent 0)",
            backgroundSize: "24px 24px"
          }}
        ></div>
        <div className="absolute top-0 right-0 w-[800px] h-[800px] bg-primary rounded-full blur-[120px] opacity-20 -translate-y-1/2 translate-x-1/3 pointer-events-none"></div>
        <div className="absolute bottom-0 left-0 w-[600px] h-[600px] bg-inverse-primary rounded-full blur-[100px] opacity-10 translate-y-1/4 -translate-x-1/4 pointer-events-none"></div>
        
        {/* Top Branding */}
        <div className="relative z-10 flex items-center space-x-3">
          <div className="w-10 h-10 bg-on-primary rounded flex items-center justify-center shadow-sm">
            <span className="material-symbols-outlined text-primary text-2xl" style={{ fontVariationSettings: "'FILL' 1" }}>hub</span>
          </div>
          <h1 className="font-h1 text-h1 text-on-primary font-bold tracking-tight">TelcoX CRM</h1>
        </div>

        {/* Main Tagline */}
        <div className="relative z-10 max-w-lg mt-auto mb-24">
          <h2 className="font-h1 text-[40px] leading-[1.1] text-on-primary font-bold mb-6">Enterprise CRM & Operations Management Platform.</h2>
          <p className="font-body-lg text-on-primary-container opacity-90 max-w-md">Streamline your telecommunications infrastructure, manage customer lifecycles, and optimize network deployment with precision.</p>
        </div>

        {/* Footer Info */}
        <div className="relative z-10 flex items-center justify-between text-on-primary-container opacity-75 font-body-sm border-t border-on-primary/20 pt-6">
          <span>© 2024 TelcoX Communications Inc.</span>
          <div className="flex space-x-6">
            <a href="#" className="hover:text-on-primary transition-colors">System Status: <span className="text-secondary-fixed font-mono-label ml-1">ALL SYSTEMS OPERATIONAL</span></a>
          </div>
        </div>
      </div>

      {/* Right Side: Login Panel */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-12 relative">
        {/* Mobile Logo */}
        <div className="absolute top-8 left-8 flex lg:hidden items-center space-x-2">
          <div className="w-8 h-8 bg-primary rounded flex items-center justify-center">
            <span className="material-symbols-outlined text-on-primary text-xl" style={{ fontVariationSettings: "'FILL' 1" }}>hub</span>
          </div>
          <span className="font-h2 text-h2 text-on-surface font-bold">TelcoX</span>
        </div>

        {/* Login Card */}
        <div className="w-full max-w-[440px] bg-surface rounded-xl border border-outline-variant p-8 shadow-sm relative z-10">
          <div className="mb-8">
            <h2 className="font-h1 text-h1 text-on-surface mb-2">Secure Access</h2>
            <p className="font-body-md text-on-surface-variant">Please authenticate with your corporate credentials to access the enterprise console.</p>
          </div>

          {error && (
            <div className="mb-6 p-3 bg-error-container text-on-error-container text-body-sm rounded-lg flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px]">error</span>
              {error}
            </div>
          )}

          <form className="space-y-6" onSubmit={handleSubmit}>
            {/* Employee ID / Username */}
            <div className="space-y-1.5">
              <label htmlFor="employeeId" className="block font-label-sm text-secondary">Employee ID / Username</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <span className="material-symbols-outlined text-outline text-[18px]">badge</span>
                </div>
                <input 
                  type="text" 
                  id="employeeId" 
                  name="employeeId" 
                  placeholder="TX-0000" 
                  required 
                  autoComplete="username"
                  className="block w-full pl-10 pr-3 py-2 h-10 bg-surface border border-outline-variant rounded-lg font-mono-id text-on-surface placeholder:text-outline focus:ring-0 focus:border-primary transition-colors"
                />
              </div>
            </div>

            {/* Password */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label htmlFor="password" className="block font-label-sm text-secondary">Password</label>
                <a href="#" className="font-label-sm text-primary hover:text-primary-container transition-colors">Forgot password?</a>
              </div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <span className="material-symbols-outlined text-outline text-[18px]">lock</span>
                </div>
                <input 
                  type={showPassword ? "text" : "password"} 
                  id="password" 
                  name="password" 
                  placeholder="••••••••" 
                  required 
                  autoComplete="current-password"
                  className="block w-full pl-10 pr-10 py-2 h-10 bg-surface border border-outline-variant rounded-lg font-body-md text-on-surface placeholder:text-outline focus:ring-0 focus:border-primary transition-colors"
                />
                <button 
                  type="button" 
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-outline hover:text-on-surface-variant transition-colors"
                >
                  <span className="material-symbols-outlined text-[18px]">{showPassword ? "visibility" : "visibility_off"}</span>
                </button>
              </div>
            </div>

            {/* Remember Me */}
            <div className="flex items-center justify-between pt-2">
              <div className="flex items-center">
                <input 
                  type="checkbox" 
                  id="remember" 
                  name="remember" 
                  className="h-4 w-4 rounded border-outline-variant text-primary focus:ring-primary focus:ring-offset-0 bg-surface cursor-pointer"
                />
                <label htmlFor="remember" className="ml-2 block font-body-sm text-on-surface-variant cursor-pointer">
                  Remember this device
                </label>
              </div>
            </div>

            {/* Submit Action */}
            <div className="pt-2">
              <button 
                type="submit" 
                disabled={loading}
                className="w-full flex justify-center items-center h-10 px-4 border border-transparent rounded-lg shadow-sm font-label-md text-on-primary bg-primary hover:bg-primary-container focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary transition-colors disabled:opacity-70"
              >
                {loading ? (
                  <>
                    <span className="material-symbols-outlined mr-2 text-[18px] animate-spin">sync</span>
                    Authenticating...
                  </>
                ) : (
                  <>
                    Login to Console
                    <span className="material-symbols-outlined ml-2 text-[18px]">arrow_forward</span>
                  </>
                )}
              </button>
            </div>

            {/* Divider */}
            <div className="relative py-4">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-outline-variant"></div>
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-surface font-label-sm text-secondary">OR</span>
              </div>
            </div>

            {/* SSO Login */}
            <button type="button" className="w-full flex justify-center items-center h-10 px-4 border border-outline-variant rounded-lg bg-surface font-label-md text-on-surface hover:bg-surface-container-low transition-colors">
              <span className="material-symbols-outlined mr-2 text-[18px] text-primary">dns</span>
              Login with Corporate SSO
            </button>
          </form>

          {/* Bottom Legal/Security Note */}
          <div className="mt-8 pt-6 border-t border-outline-variant text-center">
            <div className="flex items-center justify-center space-x-1 text-secondary font-body-sm mb-2">
              <span className="material-symbols-outlined text-[14px]">shield</span>
              <span>Secure Enterprise Access</span>
            </div>
            <div className="flex items-center justify-center space-x-4 font-body-sm text-outline">
              <a href="#" className="hover:text-on-surface transition-colors">Privacy Policy</a>
              <span className="w-1 h-1 rounded-full bg-outline-variant"></span>
              <a href="#" className="hover:text-on-surface transition-colors">Terms of Service</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
