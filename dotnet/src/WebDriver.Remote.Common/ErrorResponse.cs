﻿using System.Collections.Generic;
using Newtonsoft.Json;

namespace OpenQA.Selenium.Remote
{
    /// <summary>
    /// Provides a way to store errors from a repsonse
    /// </summary>
    public class ErrorResponse
    {
        private StackTraceElement[] stackTrace;
        private string message = string.Empty;
        private string className = string.Empty;
        private string screenshot = string.Empty;

        /// <summary>
        /// Initializes a new instance of the <see cref="ErrorResponse"/> class.
        /// </summary>
        public ErrorResponse()
        {
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="ErrorResponse"/> class using the specified values.
        /// </summary>
        /// <param name="responseValue">A <see cref="Dictionary{K, V}"/> containing names and values of
        /// the properties of this <see cref="ErrorResponse"/>.</param>
        public ErrorResponse(Dictionary<string, object> responseValue)
        {
            if (responseValue.ContainsKey("message"))
            {
                this.message = responseValue["message"].ToString();
            }

            if (responseValue.ContainsKey("screen"))
            {
                this.screenshot = responseValue["screen"].ToString();
            }

            if (responseValue.ContainsKey("class"))
            {
                this.className = responseValue["class"].ToString();
            }

            if (responseValue.ContainsKey("stackTrace"))
            {
                object[] stackTraceArray = responseValue["stackTrace"] as object[];
                if (stackTraceArray != null)
                {
                    List<StackTraceElement> stackTraceList = new List<StackTraceElement>();
                    foreach (object rawStackTraceElement in stackTraceArray)
                    {
                        Dictionary<string, object> elementAsDictionary = rawStackTraceElement as Dictionary<string, object>;
                        if (elementAsDictionary != null)
                        {
                            stackTraceList.Add(new StackTraceElement(elementAsDictionary));
                        }
                    }

                    this.stackTrace = stackTraceList.ToArray();
                }
            }
        }

        /// <summary>
        /// Gets or sets the message from the reponse
        /// </summary>
        [JsonProperty("message")]
        public string Message
        {
            get { return this.message; }
            set { this.message = value; }
        }

        /// <summary>
        /// Gets or sets the class name that threw the error
        /// </summary>
        [JsonProperty("class")]
        public string ClassName
        {
            get { return this.className; }
            set { this.className = value; }
        }

        /// <summary>
        /// Gets or sets the screenshot of the error
        /// </summary>
        [JsonProperty("screen")]
        public string Screenshot
        {
            // TODO: (JimEvans) Change this to return an Image.
            get { return this.screenshot; }
            set { this.screenshot = value; }
        }

        /// <summary>
        /// Gets or sets the stack trace of the error
        /// </summary>
        [JsonProperty("stackTrace")]
        public StackTraceElement[] StackTrace
        {
            get { return this.stackTrace; }
            set { this.stackTrace = value; }
        }
    }
}
